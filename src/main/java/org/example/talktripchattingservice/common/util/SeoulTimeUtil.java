package org.example.talktripchattingservice.common.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class SeoulTimeUtil {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private SeoulTimeUtil() {}

    public static LocalDateTime now() {
        return LocalDateTime.now(SEOUL);
    }

    public static Timestamp nowAsTimestamp() {
        return Timestamp.valueOf(now());
    }
}

