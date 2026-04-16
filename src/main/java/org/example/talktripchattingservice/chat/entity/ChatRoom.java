package org.example.talktripchattingservice.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.talktripchattingservice.chat.enums.RoomType;
import org.example.talktripchattingservice.chat.enums.RoomTypeJpaConverter;
import org.example.talktripchattingservice.common.entity.BaseEntity;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "chatting_room_tab")
public class ChatRoom extends BaseEntity {

    @Id
    @Column(name = "room_id", nullable = false, length = 255)
    private String roomId;

    @Column(name = "title", nullable = true, length = 255)
    private String title;

    @Column(name = "product_id", nullable = false)
    private int productId;

    @Convert(converter = RoomTypeJpaConverter.class)
    @Column(name = "room_type", nullable = false, length = 32)
    private RoomType roomType;
}

