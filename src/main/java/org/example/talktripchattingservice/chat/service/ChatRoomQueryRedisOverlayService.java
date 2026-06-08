package org.example.talktripchattingservice.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.talktripchattingservice.chat.dto.response.ChatRoomDTO;
import org.example.talktripchattingservice.chat.redis.ChatRoomRedisSummaryService;
import org.example.talktripchattingservice.common.dto.SliceResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RDB로 목록(정렬/페이지네이션)을 가져오되, "마지막 메시지"를 Redis Hash summary 캐시로 덮어쓰는 비교용 서비스.
 *
 * <p>목표: RDB join/정렬 비용이 커질 때 Redis summary 캐시가 효과가 있는지 수치화.</p>
 */
@Service
@RequiredArgsConstructor
public class ChatRoomQueryRedisOverlayService {

    private final ChatRoomQueryService chatRoomQueryService;
    private final ChatRoomRedisSummaryService chatRoomRedisSummaryService;

    public SliceResponse<ChatRoomDTO> getRoomsOverlayLastMessage(String accountEmail, Integer limit, String cursor) {
        SliceResponse<ChatRoomDTO> base = chatRoomQueryService.getRooms(accountEmail, limit, cursor);

        List<ChatRoomDTO> items = base.items().stream()
                .map(dto -> {
                    String last = chatRoomRedisSummaryService.getLastMessageBody(dto.getRoomId())
                            .orElse(dto.getLastMessage());
                    return new ChatRoomDTO(
                            dto.getRoomId(),
                            null,
                            dto.getCreatedAt(),
                            dto.getUpdatedAt(),
                            dto.getTitle(),
                            last,
                            dto.getNotReadMessageCount(),
                            dto.getRoomType()
                    );
                })
                .toList();

        return SliceResponse.of(items, base.nextCursor(), base.hasNext());
    }
}

