package org.example.talktripchattingservice.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
public class ChatMessagePush {
    private String messageId;
    private String roomId;
    private String sender;
    private String senderName;
    private String message;
    private String createdAt;

    @JsonCreator
    public ChatMessagePush(
            @JsonProperty("messageId") String messageId,
            @JsonProperty("roomId") String roomId,
            @JsonProperty("sender") String sender,
            @JsonProperty("senderName") String senderName,
            @JsonProperty("message") String message,
            @JsonProperty("createdAt") String createdAt
    ) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.sender = sender;
        this.senderName = senderName;
        this.message = message;
        this.createdAt = createdAt;
    }
}

