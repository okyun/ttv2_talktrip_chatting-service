package org.example.talktripchattingservice.chat.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.example.talktripchattingservice.chat.config.ChatRedisProperties;
import org.example.talktripchattingservice.chat.dto.response.ChatMessagePush;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 멀티 인스턴스 채팅을 위한 Redis Pub/Sub 브릿지.
 *
 * - publishToOtherInstances: senderInstanceId를 감싸서 발행
 * - onMessage: senderInstanceId가 자기 자신이면 무시, 아니면 WebSocket으로 fan-out
 */
@Component
public class RedisMessageBroker implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageBroker.class);

    private final RedisMessageListenerContainer container;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRedisProperties props;
    private final String instanceId = UUID.randomUUID().toString();

    public RedisMessageBroker(
            RedisMessageListenerContainer container,
            @Qualifier("chatRedisTemplate") RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate,
            ChatRedisProperties props
    ) {
        this.container = container;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
        this.props = props;
    }

    @PostConstruct
    public void subscribe() {
        String prefix = props.pubsubPrefix();
        container.addMessageListener(this, new PatternTopic(prefix + ":room:*"));
        logger.info("[RedisMessageBroker] subscribe pattern: {}:room:* (instanceId={})", prefix, instanceId);
    }

    public void publishToOtherInstances(String channel, Object payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            String wrapped = "{\"senderInstanceId\":\"" + instanceId + "\",\"originalMessage\":" + message + "}";
            redisTemplate.convertAndSend(channel, wrapped);
        } catch (Exception e) {
            logger.error("Redis publish failed: channel={}, error={}", channel, e.getMessage(), e);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String payload = new String(message.getBody());
        try {
            JsonNode root = objectMapper.readTree(payload);
            String senderInstanceId = root.path("senderInstanceId").asText(null);
            if (senderInstanceId != null && senderInstanceId.equals(instanceId)) return;

            JsonNode original = root.path("originalMessage");
            if (channel.contains(":room:")) {
                ChatMessagePush dto = objectMapper.treeToValue(original, ChatMessagePush.class);
                String dest = "/topic/chat/room/" + dto.getRoomId();
                messagingTemplate.convertAndSend(dest, dto);
            }
        } catch (Exception e) {
            logger.warn("Redis message parse failed: channel={}, error={}", channel, e.getMessage());
        }
    }
}

