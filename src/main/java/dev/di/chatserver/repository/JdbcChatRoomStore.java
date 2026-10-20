package dev.di.chatserver.repository;

import dev.di.chatserver.domain.ChatRoom;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!memory")
public class JdbcChatRoomStore implements ChatRoomStore {
    private final JdbcTemplate jdbc;

    public JdbcChatRoomStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private ChatRoom map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new ChatRoom(rs.getLong("id"), rs.getLong("product_id"),
                rs.getLong("buyer_id"), rs.getLong("seller_id"));
    }

    @Override
    public ChatRoom save(Long productId, Long buyerId, Long sellerId) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO chat_rooms(product_id, buyer_id, seller_id) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, productId);
            statement.setLong(2, buyerId);
            statement.setLong(3, sellerId);
            return statement;
        }, keys);
        return new ChatRoom(keys.getKey().longValue(), productId, buyerId, sellerId);
    }

    @Override
    public Optional<ChatRoom> findById(Long id) {
        return jdbc.query("SELECT * FROM chat_rooms WHERE id = ?", this::map, id)
                .stream().findFirst();
    }

    @Override
    public Optional<ChatRoom> findByProductIdAndBuyerIdAndSellerId(
            Long productId, Long buyerId, Long sellerId) {
        return jdbc.query("SELECT * FROM chat_rooms WHERE product_id = ? AND buyer_id = ? AND seller_id = ?",
                this::map, productId, buyerId, sellerId).stream().findFirst();
    }

    @Override
    public List<ChatRoom> findAllByMemberId(Long memberId) {
        return jdbc.query("SELECT * FROM chat_rooms WHERE buyer_id = ? OR seller_id = ? ORDER BY id DESC",
                this::map, memberId, memberId);
    }
}
