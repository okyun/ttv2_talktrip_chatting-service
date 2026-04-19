package org.example.talktripchattingservice.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.talktripchattingservice.chat.config.ChatRedisProperties;
import org.example.talktripchattingservice.chat.dto.response.ChatRoomDTO;
import org.example.talktripchattingservice.chat.enums.RoomType;
import org.example.talktripchattingservice.chat.redis.ChatRoomRedisIndexService;
import org.example.talktripchattingservice.chat.repository.ChatMessageRepository;
import org.example.talktripchattingservice.chat.repository.ChatRoomListRow;
import org.example.talktripchattingservice.chat.repository.ChatRoomMetadataRow;
import org.example.talktripchattingservice.chat.repository.ChatRoomRepository;
import org.example.talktripchattingservice.chat.repository.RoomUnreadRow;
import org.example.talktripchattingservice.common.dto.SliceResponse;
import org.example.talktripchattingservice.common.util.CursorUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomQueryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRedisProperties chatRedisProperties;
    private final ChatRoomRedisIndexService chatRoomRedisIndexService;

    public SliceResponse<ChatRoomDTO> getRooms(String accountEmail, Integer limit, String cursor) {
        int size = (limit == null || limit <= 0 || limit > 200) ? 50 : limit;

        if (chatRedisProperties.roomListIndexEnabled()) {
            ensureRedisRoomIndexMatchesDb(accountEmail);
            List<String> lexMembers = chatRoomRedisIndexService.loadOrderedLexMembersPage(accountEmail, cursor, size + 1);
            if (lexMembers.isEmpty()) {
                return SliceResponse.of(List.of(), null, false);
            }
            List<ChatRoomDTO> all = buildDtosFromLexMembers(accountEmail, lexMembers);
            List<ChatRoomDTO> items = all.size() > size ? all.subList(0, size) : all;
            boolean hasNext = lexMembers.size() > size;
            String nextCursor = null;
            if (hasNext && !items.isEmpty()) {
                ChatRoomDTO last = items.get(items.size() - 1);
                nextCursor = CursorUtil.encode(last.getUpdatedAt(), last.getRoomId());
            }
            return SliceResponse.of(items, nextCursor, hasNext);
        }

        LocalDateTime cursorUpdatedAt = null;
        String cursorRoomId = null;
        if (cursor != null && !cursor.isBlank()) {
            // cursor = base64("updatedAt|roomId")
            CursorUtil.Cursor decoded = CursorUtil.decode(cursor);
            cursorUpdatedAt = decoded.createdAt();
            cursorRoomId = decoded.sequence();
        }

        // keyset pagination: size+1로 받아서 hasNext 판단
        List<ChatRoomDTO> all = chatRoomRepository.findRoomsWithLastMessageByMemberIdCursor(
                        accountEmail,
                        cursorUpdatedAt,
                        cursorRoomId,
                        PageRequest.of(0, size + 1)
                ).stream()
                .map(ChatRoomQueryService::toChatRoomDto)
                .toList();

        List<ChatRoomDTO> items = all.size() > size ? all.subList(0, size) : all;
        boolean hasNext = all.size() > size;
        String nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            ChatRoomDTO last = items.get(items.size() - 1);
            nextCursor = CursorUtil.encode(last.getUpdatedAt(), last.getRoomId());
        }
        return SliceResponse.of(items, nextCursor, hasNext);
    }

    public List<ChatRoomDTO> getAllRooms(String accountEmail, String roomType) {
        List<ChatRoomDTO> all;
        if (chatRedisProperties.roomListIndexEnabled()) {
            ensureRedisRoomIndexMatchesDb(accountEmail);
            List<String> lexMembers = chatRoomRedisIndexService.loadAllOrderedLexMembers(accountEmail);
            all = lexMembers.isEmpty()
                    ? List.of()
                    : buildDtosFromLexMembers(accountEmail, lexMembers);
        } else {
            all = chatRoomRepository.findRoomsWithLastMessageByMemberId(accountEmail).stream()
                    .map(ChatRoomQueryService::toChatRoomDto)
                    .toList();
        }
        if (roomType == null || roomType.isBlank()) return all;
        RoomType rt = RoomType.fromStoredValue(roomType);
        return all.stream().filter(r -> r.getRoomType() == rt).toList();
    }

    public int getCountAllUnreadMessages(String accountEmail) {
        return chatMessageRepository.countUnreadMessages(accountEmail);
    }

    /**
     * DB의 활성 방 개수와 Redis ZSET 원소 개수가 다르면(수동 DB 수정·캐시 플러시 등) 전체 재빌드.
     */
    private void ensureRedisRoomIndexMatchesDb(String accountEmail) {
        Long dbCountBox = chatRoomRepository.countActiveRoomsForMember(accountEmail);
        long dbCount = dbCountBox == null ? 0L : dbCountBox.longValue();
        long zc = chatRoomRedisIndexService.zCardMemberRooms(accountEmail);
        if (dbCount != zc) {
            chatRoomRedisIndexService.rebuildMemberIndexFromDb(accountEmail);
        }
    }

    private List<ChatRoomDTO> buildDtosFromLexMembers(String accountEmail, List<String> lexMembers) {
        List<String> roomIds = lexMembers.stream().map(chatRoomRedisIndexService::decodeRoomId).toList();
        if (roomIds.isEmpty()) {
            return List.of();
        }
        Map<String, ChatRoomMetadataRow> metaById = chatRoomRepository.findRoomMetadataByMemberAndRoomIds(accountEmail, roomIds)
                .stream()
                .collect(Collectors.toMap(ChatRoomMetadataRow::getRoomId, r -> r, (a, b) -> a));
        Map<String, Long> unreadById = chatRoomRepository.countUnreadByMemberAndRoomIds(accountEmail, roomIds)
                .stream()
                .collect(Collectors.toMap(
                        RoomUnreadRow::getRoomId,
                        r -> r.getCnt() == null ? 0L : r.getCnt().longValue(),
                        (a, b) -> a
                ));

        List<ChatRoomDTO> out = new ArrayList<>(lexMembers.size());
        for (String lex : lexMembers) {
            String roomId = chatRoomRedisIndexService.decodeRoomId(lex);
            ChatRoomMetadataRow meta = metaById.get(roomId);
            if (meta == null) {
                continue;
            }
            String last = chatRoomRedisIndexService.getLastMessageBody(roomId).orElse("");
            long unread = unreadById.getOrDefault(roomId, 0L);
            out.add(new ChatRoomDTO(
                    meta.getRoomId(),
                    meta.getRoomAccountId(),
                    meta.getCreatedAt(),
                    meta.getUpdatedAt(),
                    meta.getTitle(),
                    last,
                    unread,
                    RoomType.fromStoredValue(meta.getRoomType())
            ));
        }
        return out;
    }

    private static ChatRoomDTO toChatRoomDto(ChatRoomListRow row) {
        Long unread = row.getNotReadMessageCount();
        return new ChatRoomDTO(
                row.getRoomId(),
                row.getRoomAccountId(),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                row.getTitle(),
                row.getLastMessage(),
                unread == null ? 0L : unread,
                RoomType.fromStoredValue(row.getRoomType())
        );
    }
}

