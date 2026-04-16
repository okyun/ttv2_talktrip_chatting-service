package org.example.talktripchattingservice.chat.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.talktripchattingservice.chat.entity.ChattingMessageHistory;
import org.example.talktripchattingservice.common.util.SeoulTimeUtil;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class ChatMessageRequestDto {
    private String roomId;
    private String accountEmail;
    private String message;

    @JsonCreator
    public ChatMessageRequestDto(
            @JsonProperty("roomId") String roomId,
            @JsonProperty("accountEmail") String accountEmail,
            @JsonProperty("message") String message
    ) {
        this.roomId = roomId;
        this.accountEmail = accountEmail;
        this.message = message;
    }

    public ChattingMessageHistory toEntity(String accountEmail, Long sequenceNumber) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String messageId = "mgs" + uuid.substring(0, 7);
        return new ChattingMessageHistory(
                messageId,
                this.roomId,
                accountEmail,
                this.message,
                sequenceNumber,
                SeoulTimeUtil.now()
        );
    }
}

