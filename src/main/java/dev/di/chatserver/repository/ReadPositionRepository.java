package dev.di.chatserver.repository;

import org.springframework.stereotype.Repository;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ReadPositionRepository {
    private record Key(Long roomId, Long memberId) {}
    private final ConcurrentHashMap<Key, Long> positions = new ConcurrentHashMap<>();

    // 같은 회원의 요청이 동시에 도착해도 읽은 위치는 뒤로 가지 않습니다.
    public Long advance(Long roomId, Long memberId, Long messageId) {
        return positions.merge(new Key(roomId, memberId), messageId, Math::max);
    }

    public Long find(Long roomId, Long memberId) {
        return positions.getOrDefault(new Key(roomId, memberId), 0L);
    }
}
