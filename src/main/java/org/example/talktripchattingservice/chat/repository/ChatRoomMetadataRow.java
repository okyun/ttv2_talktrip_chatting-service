package org.example.talktripchattingservice.chat.repository;

import java.time.LocalDateTime;

/**
 * Redis 목록 조합용: 방 메타(제목·생성일 등)만 IN 조회할 때 네이티브 매핑.
 */
public interface ChatRoomMetadataRow {

    String getRoomId();

    String getRoomAccountId();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();

    String getTitle();

    String getRoomType();
}
