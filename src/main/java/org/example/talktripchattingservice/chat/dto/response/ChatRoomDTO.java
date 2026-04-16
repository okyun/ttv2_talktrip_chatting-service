package org.example.talktripchattingservice.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.talktripchattingservice.chat.enums.RoomType;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ChatRoomDTO {
    private String roomId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    private String title;
    private String lastMessage;
    private Long notReadMessageCount;
    private RoomType roomType;

    public ChatRoomDTO(
            String roomId,
            String roomAccountId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String title,
            String lastMessage,
            Long notReadMessageCount,
            RoomType roomType
    ) {
        this.roomId = roomId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.title = title;
        this.lastMessage = lastMessage;
        this.notReadMessageCount = notReadMessageCount;
        this.roomType = roomType;
    }

    public void setNotReadMessageCount(Long notReadMessageCount) {
        this.notReadMessageCount = notReadMessageCount;
    }
}

