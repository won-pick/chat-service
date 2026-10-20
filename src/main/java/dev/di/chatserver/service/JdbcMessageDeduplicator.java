package dev.di.chatserver.service;

import dev.di.chatserver.domain.ChatMessage;
import dev.di.chatserver.exception.ChatRequestException;
import dev.di.chatserver.exception.ErrorCode;
import dev.di.chatserver.repository.ChatMessageStore;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Service
@Profile("!memory")
public class JdbcMessageDeduplicator implements MessageDeduplicator {
    private record SavedRequest(String content, Long messageId) {}

    private final JdbcTemplate jdbc;
    private final ChatMessageStore messages;
    private final TransactionTemplate transactions;

    public JdbcMessageDeduplicator(JdbcTemplate jdbc, ChatMessageStore messages,
                                   PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.messages = messages;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public synchronized ChatMessage execute(Long roomId, Long senderId, String key,
                                            String content, Supplier<ChatMessage> action) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new ChatRequestException(400, ErrorCode.INVALID_INPUT,
                    "Idempotency-Key는 영문·숫자·._:-로 이루어진 1~128자여야 합니다.");
        }
        return transactions.execute(status -> {
            var existing = jdbc.query("SELECT content, message_id FROM chat_message_requests " +
                            "WHERE room_id = ? AND sender_id = ? AND request_key = ?",
                    (rs, row) -> new SavedRequest(rs.getString(1), rs.getLong(2)),
                    roomId, senderId, key).stream().findFirst();
            if (existing.isPresent()) {
                if (!existing.get().content().equals(content)) {
                    throw new ChatRequestException(409, ErrorCode.IDEMPOTENCY_CONFLICT,
                            "같은 Idempotency-Key로 다른 메시지를 보낼 수 없습니다.");
                }
                return messages.findById(existing.get().messageId()).orElseThrow();
            }
            ChatMessage message = action.get();
            jdbc.update("INSERT INTO chat_message_requests(room_id, sender_id, request_key, " +
                            "content, message_id) VALUES (?, ?, ?, ?, ?)",
                    roomId, senderId, key, content, message.getId());
            return message;
        });
    }
}
