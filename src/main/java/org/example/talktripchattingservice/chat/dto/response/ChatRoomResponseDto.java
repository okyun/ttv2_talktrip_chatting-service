package org.example.talktripchattingservice.chat.dto.response;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ChatRoomResponseDto {
    private final String roomId;
    private final String title;

    public ChatRoomResponseDto(String roomId) {
        this.roomId = roomId;
        this.title = null;
    }

    public ChatRoomResponseDto(String roomId, String title) {
        this.roomId = roomId;
        this.title = title;
    }

    public static ChatRoomResponseDto createNew() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return new ChatRoomResponseDto("room_" + uuid.substring(0, 12), null);
    }
}

