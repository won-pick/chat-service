package dev.di.chatserver.exception;

public class ChatMessageNotFoundException extends RuntimeException {
    public ChatMessageNotFoundException() {
        super("해당 채팅방에서 메시지를 찾을 수 없습니다.");
    }
}
