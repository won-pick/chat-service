package dev.di.chatserver.exception;

public class ChatAccessDeniedException extends RuntimeException {

    public ChatAccessDeniedException() {
        super("해당 채팅방의 참여자만 이용할 수 있습니다.");
    }
}