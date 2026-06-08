package org.example.talktripchattingservice.chat.redis;

import lombok.RequiredArgsConstructor;
import org.example.talktripchattingservice.chat.entity.ChatRoom;
import org.example.talktripchattingservice.chat.repository.ChatMessageRepository;
import org.example.talktripchattingservice.chat.repository.ChatRoomRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 채팅방 요약 캐시: 방별 마지막 메시지 미리보기(Hash).
 *
 * <p>목록 정렬/페이지네이션은 DB가 책임지고, Redis는 summary 캐시만 담당합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class ChatRoomRedisSummaryService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private static final String LAST_FIELD_BODY = "body";
    private static final String LAST_FIELD_AT = "at";

    @Qualifier("chatRedisTemplate")
    private final RedisTemplate<String, String> redisTemplate;
    private final ChatRedisKeys chatRedisKeys;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    public void onMessageSent(String roomId, LocalDateTime updatedAt, String messageBody) {
        writeLastMessage(roomId, messageBody, updatedAt);
    }

    public void onRoomCreated(String roomId, LocalDateTime updatedAt) {
        writeLastMessage(roomId, "", updatedAt);
    }

    /**
     * summary miss 또는 Redis flush 이후 단건 복구용.
     */
    public void syncSingleRoomFromDb(String roomId) {
        String rid = requireRoomId(roomId);
        ChatRoom room = chatRoomRepository.findById(rid).orElse(null);
        if (room == null) {
            return;
        }
        List<String> lastRows = chatMessageRepository.findLatestMessageTextByRoomId(rid);
        String lastBody = lastRows.isEmpty() ? "" : (lastRows.getFirst() == null ? "" : lastRows.getFirst());
        LocalDateTime ua = room.getUpdatedAt() != null ? room.getUpdatedAt() : LocalDateTime.now(SEOUL);
        writeLastMessage(rid, lastBody, ua);
    }

    /**
     * 마지막 메시지 본문 조회 ({@code ho.get}).
     *
     * <pre>
     * redis-cli HGET "chat:cache:rId:&lt;roomId&gt;:last" body
     * </pre>
     */
    public java.util.Optional<String> getLastMessageBody(String roomId) {
        String rid = requireRoomId(roomId);
        HashOperations<String, String, String> ho = redisTemplate.opsForHash();
        String key = Objects.requireNonNull(lastHashKey(rid));
        // ho.get(key, "body") == redis-cli HGET "<key>" body
        String v = ho.get(key, LAST_FIELD_BODY);
        return v == null ? java.util.Optional.empty() : java.util.Optional.of(v);
    }

    /**
     * Redis Hash summary 쓰기 ({@code ho.putAll}).
     *
     * <p>키: {@code chat:cache:rId:<roomId>:last} (Cluster면 {@code chat:cache:rId:{chatRoom:<roomId>}:last})</p>
     *
     * <pre>
     * redis-cli HSET "chat:cache:rId:&lt;roomId&gt;:last" body "&lt;messageBody&gt;" at "&lt;updatedAt&gt;"
     * redis-cli HGETALL "chat:cache:rId:&lt;roomId&gt;:last"
     * </pre>
     */
    private void writeLastMessage(String roomId, String body, LocalDateTime at) {
        String rid = requireRoomId(roomId);
        String key = Objects.requireNonNull(lastHashKey(rid));
        Map<String, String> m = new HashMap<>();
        m.put(LAST_FIELD_BODY, body == null ? "" : body);
        m.put(LAST_FIELD_AT, at == null ? "" : at.toString());
        HashOperations<String, String, String> ho = redisTemplate.opsForHash();
        ho.putAll(key, m);
    }

    private String lastHashKey(String roomId) {
        return chatRedisKeys.roomLastHashKey(requireRoomId(roomId));
    }

    private static String requireRoomId(String roomId) {
        return Objects.requireNonNull(roomId, "roomId");
    }
}

