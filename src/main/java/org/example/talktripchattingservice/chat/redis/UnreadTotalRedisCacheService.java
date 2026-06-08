package org.example.talktripchattingservice.chat.redis;

import lombok.RequiredArgsConstructor;
import org.example.talktripchattingservice.chat.config.ChatRedisProperties;
import org.example.talktripchattingservice.chat.service.ChatRoomQueryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * "전체 unread 합산"을 Redis에 캐시해 RDB 조인/그룹바이 비용을 줄이기 위한 비교용 캐시.
 *
 * <p>주의: 이벤트 기반으로 정확도를 유지하는 구조가 아니라, 짧은 TTL 캐시입니다.</p>
 */
@Service
@RequiredArgsConstructor
public class UnreadTotalRedisCacheService {

    private static final Duration TTL = Duration.ofSeconds(10);

    @Qualifier("chatRedisTemplate")
    private final RedisTemplate<String, String> redisTemplate;
    private final ChatRedisProperties chatRedisProperties;
    private final ChatRoomQueryService chatRoomQueryService;

    public int getTotalUnreadCached(String accountEmail) {
        String k = key(accountEmail);
        String cached = redisTemplate.opsForValue().get(k);
        if (cached != null && !cached.isBlank()) {
            try {
                return Integer.parseInt(cached.trim());
            } catch (NumberFormatException ignored) {
                // fall-through to reload
            }
        }

        int fresh = chatRoomQueryService.getCountAllUnreadMessages(accountEmail);
        redisTemplate.opsForValue().set(k, Integer.toString(fresh), TTL);
        return fresh;
    }

    private String key(String accountEmail) {
        return chatRedisProperties.cachePrefix() + ":u:" + accountEmail + ":unreadTotal";
    }
}

