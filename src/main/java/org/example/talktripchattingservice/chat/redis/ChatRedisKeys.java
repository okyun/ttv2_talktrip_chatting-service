package org.example.talktripchattingservice.chat.redis;

import org.example.talktripchattingservice.chat.config.ChatRedisProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis key naming helper.
 *
 * <p>Redis Cluster에서 같은 "방(roomId)" 관련 여러 키를 멀티키로 다루려면, 동일 slot에 고정되도록
 * hash tag({ ... })를 사용하는 것이 안전합니다.</p>
 *
 * <p>단일 Redis(standalone)에서는 hash tag가 없어도 동작하므로, 현재 연결이 cluster인지 감지하고
 * (옵션으로) hash tag 사용을 자동으로 켭니다.</p>
 */
@Component
public class ChatRedisKeys {

    private final ChatRedisProperties props;
    private final RedisConnectionFactory connectionFactory;

    public ChatRedisKeys(
            ChatRedisProperties props,
            @Qualifier("chatRedisTemplate") RedisTemplate<String, String> redisTemplate
    ) {
        this.props = props;
        this.connectionFactory = redisTemplate.getConnectionFactory();
    }

    public String roomLastHashKey(String roomId) {
        return props.cachePrefix() + ":rId:" + roomKeySegment(roomId) + ":last";
    }

    public String roomUnreadHashKey(String roomId) {
        return props.cachePrefix() + ":rId:" + roomKeySegment(roomId) + ":unread";
    }

    public String roomSeqKey(String roomId) {
        return props.cachePrefix() + ":rId:" + roomKeySegment(roomId) + ":seq";
    }

    private String roomKeySegment(String roomId) {
        if (!shouldUseHashTag()) {
            return roomId;
        }
        // Redis Cluster hash tag: {<tagName>:<roomId>}
        return "{" + props.resolvedClusterChatRoomHashTagName() + ":" + roomId + "}";
    }

    private boolean shouldUseHashTag() {
        if (!props.resolvedClusterHashTagEnabled()) {
            return false;
        }
        return isClusterConnection();
    }

    private boolean isClusterConnection() {
        if (connectionFactory == null) {
            return false;
        }
        try {
            RedisClusterConnection cc = connectionFactory.getClusterConnection();
            return cc != null;
        } catch (UnsupportedOperationException ignored) {
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }
}

