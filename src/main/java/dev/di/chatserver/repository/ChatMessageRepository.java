package dev.di.chatserver.repository;

import dev.di.chatserver.domain.ChatMessage;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class ChatMessageRepository {

    private final Map<Long, ChatMessage> messages =
            new ConcurrentHashMap<>();

    private final AtomicLong sequence = new AtomicLong(0);

    public synchronized ChatMessage save(
            Long roomId, Long senderId, String content) {

        Long id = sequence.incrementAndGet();

        ChatMessage message =
                new ChatMessage(id, roomId, senderId, content);

        messages.put(id, message);

        return message;
    }

    public synchronized List<ChatMessage> findAllByRoomId(Long roomId) {
        return messages.values().stream()
                .filter(message -> message.getRoomId().equals(roomId))
                .sorted(Comparator.comparing(ChatMessage::getId))
                .toList();
    }
    public synchronized java.util.Optional<ChatMessage> findById(Long messageId) {
        return java.util.Optional.ofNullable(messages.get(messageId));
    }
}
