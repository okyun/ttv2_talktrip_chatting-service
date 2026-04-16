package org.example.talktripchattingservice.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chatting_message_history_tab",
        indexes = {
                @Index(name = "idx_msg_room_sequence_desc", columnList = "room_id, sequence_number DESC"),
                @Index(name = "idx_msg_room_created_id_desc", columnList = "room_id, created_at, message_id")
        }
)
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ChattingMessageHistory {

    @Id
    @Column(name = "message_id", nullable = false, length = 255)
    private String messageId;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @Column(name = "account_email", nullable = false)
    private String accountEmail;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ChattingMessageHistory(String messageId, String roomId, String accountEmail, String message, Long sequenceNumber, LocalDateTime createdAt) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.accountEmail = accountEmail;
        this.message = message;
        this.sequenceNumber = sequenceNumber;
        this.createdAt = createdAt;
    }
}

