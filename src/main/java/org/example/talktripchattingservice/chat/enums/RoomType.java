package org.example.talktripchattingservice.chat.enums;

import java.util.Locale;

public enum RoomType {
    BUYER_SELLER,
    SUPPORT;

    /**
     * DB·구 덤프(chat.sql)의 {@code DIRECT}/{@code GROUP} 과 JPA Enum 문자열({@code BUYER_SELLER}/{@code SUPPORT})을 모두 수용.
     */
    public static RoomType fromStoredValue(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("room_type 값이 비어 있습니다.");
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "DIRECT", "BUYER_SELLER" -> BUYER_SELLER;
            case "GROUP", "SUPPORT" -> SUPPORT;
            default -> RoomType.valueOf(s);
        };
    }
}

