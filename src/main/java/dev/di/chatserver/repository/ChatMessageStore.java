package dev.di.chatserver.repository;

import dev.di.chatserver.domain.ChatMessage;
import java.util.List;
import java.util.Optional;

public interface ChatMessageStore {
    ChatMessage save(Long roomId, Long senderId, String content);
    List<ChatMessage> findAllByRoomId(Long roomId);
    Optional<ChatMessage> findById(Long messageId);
}
