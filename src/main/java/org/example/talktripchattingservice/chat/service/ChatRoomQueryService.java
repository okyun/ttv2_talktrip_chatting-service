package org.example.talktripchattingservice.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.talktripchattingservice.chat.dto.response.ChatRoomDTO;
import org.example.talktripchattingservice.chat.enums.RoomType;
import org.example.talktripchattingservice.chat.repository.ChatMessageRepository;
import org.example.talktripchattingservice.chat.repository.ChatRoomListRow;
import org.example.talktripchattingservice.chat.repository.ChatRoomRepository;
import org.example.talktripchattingservice.common.dto.SliceResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomQueryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    public SliceResponse<ChatRoomDTO> getRooms(String accountEmail, Integer limit, String cursor) {
        int size = (limit == null || limit <= 0 || limit > 200) ? 50 : limit;
        List<ChatRoomDTO> all = chatRoomRepository.findRoomsWithLastMessageByMemberId(accountEmail).stream()
                .map(ChatRoomQueryService::toChatRoomDto)
                .toList();
        List<ChatRoomDTO> items = all.size() > size ? all.subList(0, size) : all;
        boolean hasNext = all.size() > size;
        String nextCursor = hasNext ? items.get(items.size() - 1).getRoomId() : null;
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

