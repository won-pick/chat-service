package dev.di.chatserver.service;

import dev.di.chatserver.domain.ChatMessage;
import dev.di.chatserver.exception.ChatRequestException;
import dev.di.chatserver.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Service
@Profile("memory")
public class MessageIdempotencyService implements MessageDeduplicator {
    private record Key(Long roomId, Long senderId, String value) {}
    private record Result(String content, ChatMessage message) {}
    private final Map<Key, Result> results = new HashMap<>();

    // 단일 JVM·메모리 실습 범위: 확인, 저장, 결과 기록을 한 임계 구역에서 처리합니다.
    // 실패는 기록하지 않으며, 프로세스 재시작 시 기록도 사라집니다.
    public synchronized ChatMessage execute(Long roomId, Long senderId, String key,
                                            String content, Supplier<ChatMessage> action) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,128}")) {
            throw new ChatRequestException(400, ErrorCode.INVALID_INPUT,
                    "Idempotency-Key는 영문·숫자·._:-로 이루어진 1~128자여야 합니다.");
        }
        Key scopedKey = new Key(roomId, senderId, key);
        Result existing = results.get(scopedKey);
        if (existing != null) {
            if (!existing.content().equals(content)) {
                throw new ChatRequestException(409, ErrorCode.IDEMPOTENCY_CONFLICT,
                        "같은 Idempotency-Key로 다른 메시지를 보낼 수 없습니다.");
            }
            return existing.message();
        }
        ChatMessage message = action.get();
        results.put(scopedKey, new Result(content, message));
        return message;
    }
}
