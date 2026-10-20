package dev.di.chatserver.dto;

import dev.di.chatserver.domain.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id, Long roomId, Long senderId,
        String content, LocalDateTime sentAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getRoomId(),
                message.getSenderId(), message.getContent(), message.getSentAt());
    }
}
