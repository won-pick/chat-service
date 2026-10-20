package dev.di.chatserver.domain;

import java.time.LocalDateTime;

public class ChatMessage {

    private final Long id;
    private final Long roomId;
    private final Long senderId;
    private final String content;
    private final LocalDateTime sentAt;

    public ChatMessage(Long id, Long roomId,
                       Long senderId, String content) {
        this(id, roomId, senderId, content, LocalDateTime.now());
    }

    public ChatMessage(Long id, Long roomId,
                       Long senderId, String content, LocalDateTime sentAt) {
        this.id = id;
        this.roomId = roomId;
        this.senderId = senderId;
        this.content = content;
        this.sentAt = sentAt;
    }

    public Long getId() {
        return id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
