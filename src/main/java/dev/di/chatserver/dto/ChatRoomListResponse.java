package dev.di.chatserver.dto;

// 메시지가 없는 방은 lastMessage=null, unreadCount=0입니다.
public record ChatRoomListResponse(
        Long id,
        Long productId,
        Long buyerId,
        Long sellerId,
        ChatMessageResponse lastMessage,
        long unreadCount
) {}
