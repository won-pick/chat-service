package dev.di.chatserver.service;

import dev.di.chatserver.domain.ChatMessage;
import java.util.function.Supplier;

public interface MessageDeduplicator {
    ChatMessage execute(Long roomId, Long senderId, String key,
                        String content, Supplier<ChatMessage> action);
}
