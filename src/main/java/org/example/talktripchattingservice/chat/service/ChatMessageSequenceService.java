package org.example.talktripchattingservice.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.talktripchattingservice.chat.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessageSequenceService {

    private final ChatMessageRepository chatMessageRepository;

    @Transactional(readOnly = true)
    public Long getNextSequence(String roomId) {
        Long max = chatMessageRepository.findMaxSequenceNumberByRoomId(roomId);
        return (max == null ? 0L : max) + 1L;
    }
}

