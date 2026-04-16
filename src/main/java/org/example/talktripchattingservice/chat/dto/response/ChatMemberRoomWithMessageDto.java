package org.example.talktripchattingservice.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.example.talktripchattingservice.chat.entity.ChattingMessageHistory;

import java.time.LocalDateTime;

public record ChatMemberRoomWithMessageDto(
        String messageId,
        String roomId,
        String accountEmail,
        String message,
        Long sequenceNumber,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static ChatMemberRoomWithMessageDto from(ChattingMessageHistory m) {
        return new ChatMemberRoomWithMessageDto(
                m.getMessageId(),
                m.getRoomId(),
                m.getAccountEmail(),
                m.getMessage(),
                m.getSequenceNumber(),
                m.getCreatedAt()
        );
    }
}

