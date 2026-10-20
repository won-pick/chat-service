package dev.di.chatserver.repository;
import java.util.List;
import dev.di.chatserver.domain.ChatRoom;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("memory")
public class ChatRoomRepository implements ChatRoomStore {

    private final Map<Long, ChatRoom> rooms = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public ChatRoom save(Long productId, Long buyerId, Long sellerId) {
        Long id = sequence.incrementAndGet();

        ChatRoom room = new ChatRoom(id, productId, buyerId, sellerId);
        rooms.put(id, room);

        return room;
    }

    public Optional<ChatRoom> findById(Long id) {
        return Optional.ofNullable(rooms.get(id));
    }
    public Optional<ChatRoom> findByProductIdAndBuyerIdAndSellerId(
            Long productId, Long buyerId, Long sellerId) {

        return rooms.values().stream()
                .filter(room -> room.getProductId().equals(productId))
                .filter(room -> room.getBuyerId().equals(buyerId))
                .filter(room -> room.getSellerId().equals(sellerId))
                .findFirst();
    }
    public List<ChatRoom> findAllByMemberId(Long memberId) {
        return rooms.values().stream()
                .filter(room ->
                        room.getBuyerId().equals(memberId)
                                || room.getSellerId().equals(memberId))
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .toList();
    }
}
