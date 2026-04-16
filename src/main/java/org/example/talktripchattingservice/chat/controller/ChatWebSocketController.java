package org.example.talktripchattingservice.chat.controller;

import lombok.RequiredArgsConstructor;
import org.example.talktripchattingservice.chat.dto.request.ChatMessageRequestDto;
import org.example.talktripchattingservice.chat.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@RequiredArgsConstructor
@Controller
public class ChatWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final ChatService chatService;

    @MessageMapping("/chat/message")
    public void handleMessage(ChatMessageRequestDto dto, Principal principal) {
        chatService.saveAndSend(dto, principal);
        logger.debug("WS message handled: roomId={}", dto.getRoomId());
    }
}

