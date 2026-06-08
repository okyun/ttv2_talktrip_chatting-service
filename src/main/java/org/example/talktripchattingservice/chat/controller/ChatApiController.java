package org.example.talktripchattingservice.chat.controller;

import lombok.RequiredArgsConstructor;
import org.example.talktripchattingservice.chat.dto.request.ChatRoomRequestDto;
import org.example.talktripchattingservice.chat.dto.response.ChatMemberRoomWithMessageDto;
import org.example.talktripchattingservice.chat.dto.response.ChatRoomDTO;
import org.example.talktripchattingservice.chat.dto.response.ChatRoomResponseDto;
import org.example.talktripchattingservice.chat.service.ChatRoomQueryService;
import org.example.talktripchattingservice.chat.service.ChatRoomQueryRedisOverlayService;
import org.example.talktripchattingservice.chat.service.ChatService;
import org.example.talktripchattingservice.chat.redis.UnreadTotalRedisCacheService;
import org.example.talktripchattingservice.common.dto.SliceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {

    private final ChatService chatService;
    private final ChatRoomQueryService chatRoomQueryService;
    private final ChatRoomQueryRedisOverlayService chatRoomQueryRedisOverlayService;
    private final UnreadTotalRedisCacheService unreadTotalRedisCacheService;

    @PostMapping
    public void enterChatRoom() {}

    @GetMapping("/me/chatRooms")
    public SliceResponse<ChatRoomDTO> getMyChats(
            Principal principal,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor
    ) {
        return chatRoomQueryService.getRooms(requireEmail(principal), limit, cursor);
    }

    /**
     * 비교/벤치마크용: 목록은 RDB(페이지네이션/정렬) + lastMessage는 Redis Hash 캐시로 덮어씀.
     */
    @GetMapping("/me/chatRooms/redis")
    public SliceResponse<ChatRoomDTO> getMyChatsRedisOverlay(
            Principal principal,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor
    ) {
        return chatRoomQueryRedisOverlayService.getRoomsOverlayLastMessage(requireEmail(principal), limit, cursor);
    }

    @GetMapping("/me/chatRooms/all")
    public List<ChatRoomDTO> getAllMyChats(Principal principal, @RequestParam(required = false) String roomType) {
        return chatRoomQueryService.getAllRooms(requireEmail(principal), roomType);
    }

    @GetMapping("/me/chatRooms/{roomId}/messages")
    public SliceResponse<ChatMemberRoomWithMessageDto> getRoomMessages(
            @PathVariable String roomId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "false") boolean includeMessages,
            Principal principal
    ) {
        String effectiveCursor = includeMessages ? null : cursor;
        return chatService.getRoomChattingHistoryAndMarkAsRead(roomId, requireEmail(principal), limit, effectiveCursor);
    }

    @GetMapping("/countALLUnreadMessages")
    public Map<String, Integer> getCountAllUnreadMessages(Principal principal) {
        int count = chatRoomQueryService.getCountAllUnreadMessages(requireEmail(principal));
        return Map.of("count", count);
    }

    /**
     * 비교/벤치마크용: "전체 unread 합산"을 Redis 짧은 TTL 캐시로 응답.
     */
    @GetMapping("/countALLUnreadMessages/redis")
    public Map<String, Integer> getCountAllUnreadMessagesRedis(Principal principal) {
        int count = unreadTotalRedisCacheService.getTotalUnreadCached(requireEmail(principal));
        return Map.of("count", count);
    }

    @PostMapping("/rooms/enter")
    public ResponseEntity<ChatRoomResponseDto> enterOrCreateRoom(Principal principal, @RequestBody ChatRoomRequestDto chatRoomRequestDto
    ) {
        requirePrincipal(principal);
        chatRoomRequestDto.setTitle(chatRoomRequestDto.getTitle());
        ChatRoomResponseDto resp = chatService.enterOrCreateRoom(principal, chatRoomRequestDto);
        return ResponseEntity.ok(resp);
    }

    @PatchMapping("/me/chatRooms/{roomId}/markAsRead")
    public ResponseEntity<Void> markRoomAsRead(Principal principal, @PathVariable String roomId) {
        chatService.markRoomAsRead(requireEmail(principal), roomId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/chatRooms/{roomId}")
    public ResponseEntity<Void> leaveChatRoom(Principal principal, @PathVariable String roomId) {
        chatService.markChatRoomAsDeleted(requireEmail(principal), roomId);
        return ResponseEntity.noContent().build();
    }

    private static void requirePrincipal(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인(Authorization Bearer)이 필요합니다.");
        }
    }

    private static String requireEmail(Principal principal) {
        requirePrincipal(principal);
        String name = principal.getName();
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효한 회원 이메일이 JWT에 없습니다.");
        }
        return name;
    }
}

