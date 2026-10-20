package dev.di.chatserver.repository;

import dev.di.chatserver.domain.ChatRoom;
import java.util.List;
import java.util.Optional;

public interface ChatRoomStore {
    ChatRoom save(Long productId, Long buyerId, Long sellerId);
    Optional<ChatRoom> findById(Long id);
    Optional<ChatRoom> findByProductIdAndBuyerIdAndSellerId(Long productId, Long buyerId, Long sellerId);
    List<ChatRoom> findAllByMemberId(Long memberId);
}
