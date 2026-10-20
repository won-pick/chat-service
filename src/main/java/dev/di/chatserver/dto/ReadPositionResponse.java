package dev.di.chatserver.dto;

public record ReadPositionResponse(
        Long roomId, Long memberId, Long lastReadMessageId
) {}
