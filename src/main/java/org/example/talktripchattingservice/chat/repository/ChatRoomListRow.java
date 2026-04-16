package org.example.talktripchattingservice.chat.repository;

import java.time.LocalDateTime;

/**
 * {@link ChatRoomRepository#findRoomsWithLastMessageByMemberId(String)} 네이티브 쿼리 결과 매핑.
 */
public interface ChatRoomListRow {

    String getRoomId();

    String getRoomAccountId();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();

    String getTitle();

    String getLastMessage();

    Long getNotReadMessageCount();

    String getRoomType();
}
