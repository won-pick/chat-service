package dev.di.chatserver.repository;

import dev.di.chatserver.domain.ChatMessage;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!memory")
public class JdbcChatMessageStore implements ChatMessageStore {
    private final JdbcTemplate jdbc;

    public JdbcChatMessageStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private ChatMessage map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new ChatMessage(rs.getLong("id"), rs.getLong("room_id"),
                rs.getLong("sender_id"), rs.getString("content"),
                rs.getTimestamp("sent_at").toLocalDateTime());
    }

    @Override
    public ChatMessage save(Long roomId, Long senderId, String content) {
        LocalDateTime sentAt = LocalDateTime.now();
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO chat_messages(room_id, sender_id, content, sent_at) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, roomId);
            statement.setLong(2, senderId);
            statement.setString(3, content);
            statement.setTimestamp(4, Timestamp.valueOf(sentAt));
            return statement;
        }, keys);
        return new ChatMessage(keys.getKey().longValue(), roomId, senderId, content, sentAt);
    }

    @Override
    public List<ChatMessage> findAllByRoomId(Long roomId) {
        return jdbc.query("SELECT * FROM chat_messages WHERE room_id = ? ORDER BY id", this::map, roomId);
    }

    @Override
    public Optional<ChatMessage> findById(Long messageId) {
        return jdbc.query("SELECT * FROM chat_messages WHERE id = ?", this::map, messageId)
                .stream().findFirst();
    }
}
