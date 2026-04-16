package org.example.talktripchattingservice.chat.dto.response;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ChatRoomResponseDto {
    private final String roomId;

    public ChatRoomResponseDto(String roomId) {
        this.roomId = roomId;
    }

    public static ChatRoomResponseDto createNew() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return new ChatRoomResponseDto("room_" + uuid.substring(0, 12));
    }
}

