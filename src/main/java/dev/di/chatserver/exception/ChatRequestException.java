package dev.di.chatserver.exception;

public class ChatRequestException extends RuntimeException {
    private final int status;
    private final ErrorCode code;

    public ChatRequestException(int status, ErrorCode code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public int getStatus() { return status; }
    public ErrorCode getCode() { return code; }
}
