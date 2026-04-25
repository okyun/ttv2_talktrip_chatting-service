package org.example.talktripchattingservice.chat.redis;

/**
 * 멀티 인스턴스(서버) 간 채팅 메시지 전파 브릿지.
 *
 * 구현체는 Pub/Sub 또는 Streams 기반으로 동작할 수 있다.
 */
public interface ClusterBroadcastBroker {

    void publishRoomMessage(String roomId, Object payload);
}

