package dev.di.chatserver.dto;

import dev.di.chatserver.domain.ChatRoom;

public record ChatRoomResponse(
        Long id,
        Long productId,
        Long buyerId,
        Long sellerId
) {

    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(
                room.getId(),
                room.getProductId(),
                room.getBuyerId(),
                room.getSellerId()
        );
    }
}