package org.example.talktripchattingservice.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.talktripchattingservice.chat.dto.response.ChatRoomDTO;
import org.example.talktripchattingservice.chat.enums.RoomType;
import org.example.talktripchattingservice.chat.repository.ChatMessageRepository;
import org.example.talktripchattingservice.chat.repository.ChatRoomListRow;
import org.example.talktripchattingservice.chat.repository.ChatRoomRepository;
import org.example.talktripchattingservice.common.dto.SliceResponse;
import org.example.talktripchattingservice.common.util.CursorUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomQueryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    public SliceResponse<ChatRoomDTO> getRooms(String accountEmail, Integer limit, String cursor) {
        int size = (limit == null || limit <= 0 || limit > 200) ? 50 : limit;

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
        List<ChatRoomDTO> all = chatRoomRepository.findRoomsWithLastMessageByMemberId(accountEmail).stream()
                .map(ChatRoomQueryService::toChatRoomDto)
                .toList();
        if (roomType == null || roomType.isBlank()) return all;
        RoomType rt = RoomType.fromStoredValue(roomType);
        return all.stream().filter(r -> r.getRoomType() == rt).toList();
    }

    public int getCountAllUnreadMessages(String accountEmail) {
        return chatMessageRepository.countUnreadMessages(accountEmail);
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

