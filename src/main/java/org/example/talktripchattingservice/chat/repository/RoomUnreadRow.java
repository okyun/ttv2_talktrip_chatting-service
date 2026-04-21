package org.example.talktripchattingservice.chat.repository;

/**
 * 방별 안 읽은 메시지 수 배치 조회 네이티브 매핑.
 */
public interface RoomUnreadRow {

    String getRoomId();

    /** 네이티브 COUNT(*) 매핑 시 Long/BigInteger 등이 될 수 있음 */
    Number getCnt();
}
