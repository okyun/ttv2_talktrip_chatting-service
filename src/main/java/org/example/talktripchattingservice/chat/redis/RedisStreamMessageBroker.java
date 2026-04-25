package org.example.talktripchattingservice.chat.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.example.talktripchattingservice.chat.config.ChatRedisProperties;
import org.example.talktripchattingservice.chat.dto.response.ChatMessagePush;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Redis Streams 기반 멀티 인스턴스 전파 브릿지.
 *
 * - Producer: XADD
 * - Consumer: XREADGROUP
 * - Success: XACK
 * - Failure: ACK 하지 않음(pending 유지) -> 재처리/장애 복구 가능(at-least-once)
 *
 * 주의: Pub/Sub 처럼 "즉시 fan-out"은 아니고, Polling 기반으로 처리된다.
 * 운영에서는 poll 주기/COUNT, pending reclaim 전략(예: XCLAIM) 등을 별도 정책으로 둔다.
 */
@Component
@ConditionalOnProperty(name = "chat.redis.broadcast-mode", havingValue = "streams")
public class RedisStreamMessageBroker implements ClusterBroadcastBroker {

    private static final Logger logger = LoggerFactory.getLogger(RedisStreamMessageBroker.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRedisProperties props;

    private final String instanceId = UUID.randomUUID().toString();
    private String group;
    private String consumer;
    private String streamKey;

    public RedisStreamMessageBroker(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate,
            ChatRedisProperties props
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
        this.props = props;
    }

    @PostConstruct
    public void init() {
        this.streamKey = props.resolvedStreamKey();
        this.group = props.resolvedStreamGroupPrefix() + ":" + instanceId;
        this.consumer = "c:" + instanceId;

        ensureStreamExists();
        ensureGroupExists();

        logger.info("[RedisStreamMessageBroker] ready: streamKey={}, group={}, consumer={}", streamKey, group, consumer);
    }

    private void ensureStreamExists() {
        try {
            // stream key 가 없으면 XGROUP CREATE 가 실패하므로, 무해한 init 레코드로 스트림을 만든다.
            redisTemplate.opsForStream().add(streamKey, Map.of(
                    "type", "__init__",
                    "senderInstanceId", instanceId
            ));
        } catch (Exception e) {
            logger.info("[RedisStreamMessageBroker] stream ensure skipped: {}", e.getMessage());
        }
    }

    private void ensureGroupExists() {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.latest(), group);
        } catch (Exception e) {
            // 이미 존재하는 경우 포함
            logger.info("[RedisStreamMessageBroker] group create skipped: {}", e.getMessage());
        }
    }

    @Override
    public void publishRoomMessage(String roomId, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForStream().add(streamKey, Map.of(
                    "type", "CHAT_ROOM_MESSAGE",
                    "roomId", roomId,
                    "senderInstanceId", instanceId,
                    "payload", json
            ));
        } catch (Exception e) {
            logger.error("[RedisStreamMessageBroker] XADD failed: roomId={}, error={}", roomId, e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelayString = "${chat.redis.stream-poll-delay-ms:200}")
    public void pollAndBroadcast() {
        if (streamKey == null) return;

        try {
            List<MapRecord<String, String, String>> records = redisTemplate.<String, String>opsForStream().read(
                    Consumer.from(group, consumer),
                    StreamReadOptions.empty()
                            .count(props.resolvedStreamPollCount())
                            .block(Duration.ofSeconds(1)),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed())
            );

            if (records == null || records.isEmpty()) return;

            for (MapRecord<String, String, String> record : records) {
                handleRecord(record);
            }
        } catch (Exception e) {
            logger.warn("[RedisStreamMessageBroker] XREADGROUP failed: {}", e.getMessage());
        }
    }

    private void handleRecord(MapRecord<String, String, String> record) {
        RecordId id = record.getId();
        Map<String, String> fields = record.getValue();

        String type = fields.get("type");
        if (!"CHAT_ROOM_MESSAGE".equals(type)) {
            acknowledge(id);
            return;
        }

        String sender = fields.get("senderInstanceId");
        String roomId = fields.get("roomId");
        String payload = fields.get("payload");

        try {
            // 로컬 인스턴스는 이미 WS 로 fan-out 했으므로 중복 방지
            if (sender != null && sender.equals(instanceId)) {
                acknowledge(id);
                return;
            }

            ChatMessagePush dto = parseChatMessage(payload);
            String dest = "/topic/chat/room/" + roomId;
            messagingTemplate.convertAndSend(dest, dto);

            acknowledge(id);
        } catch (Exception e) {
            logger.error("[RedisStreamMessageBroker] handle failed: id={}, roomId={}, error={}",
                    id.getValue(), roomId, e.getMessage(), e);
            // ACK 하지 않으면 pending 에 남아 재처리 가능
        }
    }

    private ChatMessagePush parseChatMessage(String payloadJson) throws Exception {
        JsonNode node = objectMapper.readTree(payloadJson);
        return objectMapper.treeToValue(node, ChatMessagePush.class);
    }

    private void acknowledge(RecordId id) {
        try {
            redisTemplate.opsForStream().acknowledge(streamKey, group, id);
        } catch (Exception e) {
            logger.warn("[RedisStreamMessageBroker] XACK failed: id={}, error={}", id.getValue(), e.getMessage());
        }
    }
}

