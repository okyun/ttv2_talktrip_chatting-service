package org.example.talktripchattingservice.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.redis")
public record ChatRedisProperties(
        String pubsubPrefix,
        String cachePrefix,
        String presencePrefix,
        String broadcastMode,
        String streamKey,
        String streamGroupPrefix,
        Integer streamPollCount,
        Long streamPollDelayMs
) {

    public String resolvedBroadcastMode() {
        return (broadcastMode == null || broadcastMode.isBlank()) ? "pubsub" : broadcastMode.trim();
    }

    public boolean useStreams() {
        return "streams".equalsIgnoreCase(resolvedBroadcastMode());
    }

    public String resolvedStreamKey() {
        return (streamKey == null || streamKey.isBlank()) ? "stream:chat:broadcast" : streamKey.trim();
    }

    public String resolvedStreamGroupPrefix() {
        return (streamGroupPrefix == null || streamGroupPrefix.isBlank()) ? "chat-broadcast" : streamGroupPrefix.trim();
    }

    public int resolvedStreamPollCount() {
        return (streamPollCount == null || streamPollCount <= 0) ? 50 : streamPollCount;
    }

    public long resolvedStreamPollDelayMs() {
        return (streamPollDelayMs == null || streamPollDelayMs < 50) ? 200L : streamPollDelayMs;
    }
}

