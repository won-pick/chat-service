package dev.di.chatserver.exception;

public class ChatRoomNotFoundException extends RuntimeException {

    public ChatRoomNotFoundException(Long roomId) {
        super("채팅방을 찾을 수 없습니다. 번호: " + roomId);
    }
}