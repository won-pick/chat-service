package dev.di.chatserver.exception;

public class SelfChatNotAllowedException extends RuntimeException {

    public SelfChatNotAllowedException() {
        super("자신의 상품에는 채팅방을 만들 수 없습니다.");
    }
}