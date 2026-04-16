package org.example.talktripchattingservice.chat.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@Table(name = "chatting_room_account_tab")
public class ChatRoomAccount {

    @Id
    @Column(name = "room_account_id")
    private String roomAccountId;

    @Column(name = "account_email")
    private String accountEmail;

    @Column(name = "room_id")
    private String roomId;

    @Column(name = "last_member_read_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastMemberReadTime;

    @Column(name = "is_del")
    private int isDel;

    @Builder
    public ChatRoomAccount(String roomAccountId, String accountEmail, String roomId) {
        this.roomAccountId = roomAccountId;
        this.accountEmail = accountEmail;
        this.roomId = roomId;
    }

    public void updateLastReadTime(LocalDateTime time) {
        this.lastMemberReadTime = time;
    }

    public static ChatRoomAccount create(String roomId, String accountEmail) {
        String roomAccountId = "RA_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return ChatRoomAccount.builder()
                .roomAccountId(roomAccountId)
                .roomId(roomId)
                .accountEmail(accountEmail)
                .build();
    }
}

