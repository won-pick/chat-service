package dev.di.chatserver.service;

import dev.di.chatserver.domain.ChatRoom;

public record CreateChatRoomResult(
        ChatRoom room,
        boolean created
) {
}