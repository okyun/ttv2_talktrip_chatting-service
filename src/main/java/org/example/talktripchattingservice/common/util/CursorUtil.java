package org.example.talktripchattingservice.common.util;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 간단 커서 인코딩/디코딩 유틸.
 *
 * 형식: base64("createdAt|sequence")
 */
public final class CursorUtil {
    private CursorUtil() {}

    public record Cursor(LocalDateTime createdAt, String sequence) {}

    public static String encode(LocalDateTime createdAt, String sequence) {
        String raw = createdAt + "|" + sequence;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String cursor) {
        String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        String[] parts = raw.split("\\|", 2);
        return new Cursor(LocalDateTime.parse(parts[0]), parts.length > 1 ? parts[1] : "0");
    }
}

