package org.example.talktripchattingservice.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.redis")
public record ChatRedisProperties(
        String pubsubPrefix,
        String cachePrefix,
        String presencePrefix,
        /** ZSET(목록 순서) + Hash(방별 마지막 메시지) 기반 내 채팅방 목록 조회 */
        boolean roomListIndexEnabled
) {
}

