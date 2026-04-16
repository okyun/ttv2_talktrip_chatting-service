package org.example.talktripchattingservice.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.talktripchattingservice.chat.dto.request.ChatMessageRequestDto;
import org.example.talktripchattingservice.chat.dto.request.ChatRoomRequestDto;
import org.example.talktripchattingservice.chat.dto.response.ChatMemberRoomWithMessageDto;
import org.example.talktripchattingservice.chat.dto.response.ChatMessagePush;
import org.example.talktripchattingservice.chat.dto.response.ChatRoomResponseDto;
import org.example.talktripchattingservice.chat.entity.ChattingMessageHistory;
import org.example.talktripchattingservice.chat.entity.ChatRoom;
import org.example.talktripchattingservice.chat.entity.ChatRoomAccount;
import org.example.talktripchattingservice.chat.enums.RoomType;
import org.example.talktripchattingservice.chat.redis.RedisMessageBroker;
import org.example.talktripchattingservice.chat.repository.ChatMessageRepository;
import org.example.talktripchattingservice.chat.repository.ChatRoomMemberRepository;
import org.example.talktripchattingservice.chat.repository.ChatRoomRepository;
import org.example.talktripchattingservice.common.dto.SliceResponse;
import org.example.talktripchattingservice.common.util.CursorUtil;
import org.example.talktripchattingservice.common.util.SeoulTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageSequenceService chatMessageSequenceService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisMessageBroker redisMessageBroker;
    private final org.example.talktripchattingservice.chat.config.ChatRedisProperties chatRedisProperties;

    @Transactional
    public void saveAndSend(ChatMessageRequestDto dto, Principal principal) {
        final String sender = principal.getName();

        if (!chatRoomMemberRepository.existsByRoomIdAndAccountEmail(dto.getRoomId(), sender)) {
            throw new AccessDeniedException("Not a member of this room: " + dto.getRoomId());
        }

        Long sequenceNumber = chatMessageSequenceService.getNextSequence(dto.getRoomId());
        ChattingMessageHistory entity = chatMessageRepository.save(dto.toEntity(sender, sequenceNumber));
        chatRoomRepository.updateUpdatedAt(dto.getRoomId(), entity.getCreatedAt());

        ChatMessagePush push = ChatMessagePush.builder()
                .messageId(entity.getMessageId())
                .roomId(entity.getRoomId())
                .sender(sender)
                .senderName(sender.contains("@") ? sender.split("@")[0] : sender)
                .message(entity.getMessage())
                .createdAt(String.valueOf(entity.getCreatedAt()))
                .build();

        String dest = "/topic/chat/room/" + dto.getRoomId();
        messagingTemplate.convertAndSend(dest, push);

        // 다른 인스턴스에도 전파
        String roomChannel = chatRedisProperties.pubsubPrefix() + ":room:" + dto.getRoomId();
        redisMessageBroker.publishToOtherInstances(roomChannel, push);

        logger.info("메시지 발송 완료: roomId={}, messageId={}", dto.getRoomId(), entity.getMessageId());
    }

    @Transactional
    public String enterOrCreateRoom(Principal principal, ChatRoomRequestDto req) {
        String buyerEmail = principal.getName();
        String sellerEmail = req.getSellerAccountEmail();

        Optional<String> existing = chatRoomMemberRepository.findRoomIdByBuyerIdAndSellerId(buyerEmail, sellerEmail);
        if (existing.isPresent()) return existing.get();

        ChatRoomResponseDto newRoomDto = ChatRoomResponseDto.createNew();
        String newRoomId = newRoomDto.getRoomId();

        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(newRoomId)
                .title("product:" + req.getProductId())
                .productId(req.getProductId())
                .roomType(RoomType.BUYER_SELLER)
                .build();
        chatRoomRepository.save(chatRoom);

        chatRoomMemberRepository.save(ChatRoomAccount.create(newRoomId, buyerEmail));
        chatRoomMemberRepository.save(ChatRoomAccount.create(newRoomId, sellerEmail));

        return newRoomId;
    }

    @Transactional
    public void markChatRoomAsDeleted(String accountEmail, String roomId) {
        chatRoomMemberRepository.updateIsDelByMemberIdAndRoomId(accountEmail, roomId, 1);
    }

    @Transactional
    public void markRoomAsRead(String accountEmail, String roomId) {
        if (!chatRoomMemberRepository.existsByRoomIdAndAccountEmail(roomId, accountEmail)) {
            throw new AccessDeniedException("Not a member of this room: " + roomId);
        }
        chatRoomMemberRepository.updateLastReadTime(roomId, accountEmail, SeoulTimeUtil.now());
    }

    @Transactional
    public SliceResponse<ChatMemberRoomWithMessageDto> getRoomChattingHistoryAndMarkAsRead(
            String roomId,
            String accountEmail,
            Integer limit,
            String cursor
    ) {
        if (!chatRoomMemberRepository.existsByRoomIdAndAccountEmail(roomId, accountEmail)) {
            throw new AccessDeniedException("Not a member of this room: " + roomId);
        }

        final int size = (limit == null || limit <= 0 || limit > 200) ? 50 : limit;
        var sort = Sort.by(Sort.Direction.DESC, "sequenceNumber", "createdAt", "messageId");
        var pageable = PageRequest.of(0, size, sort);

        List<ChattingMessageHistory> entities;
        if (cursor == null || cursor.isBlank()) {
            entities = chatMessageRepository.findFirstPage(roomId, pageable);
        } else {
            var c = CursorUtil.decode(cursor);
            entities = chatMessageRepository.findSliceBefore(roomId, Long.parseLong(c.sequence()), pageable);
        }

        chatRoomMemberRepository.updateLastReadTime(roomId, accountEmail, LocalDateTime.now());

        var items = entities.stream().map(ChatMemberRoomWithMessageDto::from).toList();
        boolean hasNext = entities.size() == size;
        String nextCursor = null;
        if (!entities.isEmpty()) {
            var last = entities.get(entities.size() - 1);
            nextCursor = CursorUtil.encode(last.getCreatedAt(), last.getSequenceNumber().toString());
        }

        return SliceResponse.of(items, hasNext ? nextCursor : null, hasNext);
    }
}

