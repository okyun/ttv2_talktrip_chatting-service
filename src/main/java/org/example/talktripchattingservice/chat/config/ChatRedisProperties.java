package org.example.talktripchattingservice.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.redis")
public record ChatRedisProperties(
        String pubsubPrefix,
        String cachePrefix,
        String presencePrefix
) {
}

