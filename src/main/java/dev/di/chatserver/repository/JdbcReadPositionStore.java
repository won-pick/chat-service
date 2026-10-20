package dev.di.chatserver.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!memory")
public class JdbcReadPositionStore implements ReadPositionStore {
    private final JdbcTemplate jdbc;

    public JdbcReadPositionStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public synchronized Long advance(Long roomId, Long memberId, Long messageId) {
        boolean exists = !jdbc.query("SELECT last_read_message_id FROM chat_read_positions " +
                        "WHERE room_id = ? AND member_id = ?", (rs, row) -> rs.getLong(1),
                roomId, memberId).isEmpty();
        if (!exists) {
            jdbc.update("INSERT INTO chat_read_positions(room_id, member_id, last_read_message_id) " +
                    "VALUES (?, ?, ?)", roomId, memberId, messageId);
        } else {
            jdbc.update("UPDATE chat_read_positions SET last_read_message_id = " +
                    "GREATEST(last_read_message_id, ?) WHERE room_id = ? AND member_id = ?",
                    messageId, roomId, memberId);
        }
        return find(roomId, memberId);
    }

    @Override
    public Long find(Long roomId, Long memberId) {
        return jdbc.query("SELECT last_read_message_id FROM chat_read_positions " +
                "WHERE room_id = ? AND member_id = ?", (rs, row) -> rs.getLong(1),
                roomId, memberId).stream().findFirst().orElse(0L);
    }
}
