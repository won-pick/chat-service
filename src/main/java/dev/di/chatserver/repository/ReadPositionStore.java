package dev.di.chatserver.repository;

public interface ReadPositionStore {
    Long advance(Long roomId, Long memberId, Long messageId);
    Long find(Long roomId, Long memberId);
}
