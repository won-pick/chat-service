package dev.di.chatserver.controller;

import dev.di.chatserver.dto.ChatMessageResponse;
import dev.di.chatserver.dto.SendMessageRequest;
import dev.di.chatserver.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/chat/rooms/{roomId}/messages")
public class ChatMessageController {
    private final ChatService chatService;
    public ChatMessageController(ChatService chatService) { this.chatService = chatService; }

    @PostMapping
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable("roomId") @Positive Long roomId,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody SendMessageRequest request) {
        var message = chatService.sendMessage(roomId, request.senderId(), request.content(), key);
        return ResponseEntity.created(URI.create("/chat/rooms/" + roomId
                        + "/messages/" + message.getId()))
                .body(ChatMessageResponse.from(message));
    }

    @GetMapping
    public List<ChatMessageResponse> getMessages(
            @PathVariable("roomId") @Positive Long roomId,
            @RequestParam("memberId") @Positive Long memberId,
            @RequestParam(value = "afterId", required = false) @PositiveOrZero Long afterId,
            @RequestParam(value = "beforeId", required = false) @Positive Long beforeId,
            @RequestParam(value = "limit", defaultValue = "50") @Min(1) @Max(100) int limit) {
        return chatService.getMessagePage(roomId, memberId, afterId, beforeId, limit)
                .stream().map(ChatMessageResponse::from).toList();
    }

    @GetMapping("/{messageId}")
    public ChatMessageResponse getMessage(
            @PathVariable("roomId") @Positive Long roomId,
            @PathVariable("messageId") @Positive Long messageId,
            @RequestParam("memberId") @Positive Long memberId) {
        return chatService.getMessages(roomId, memberId).stream()
                .filter(m -> m.getId().equals(messageId))
                .findFirst().map(ChatMessageResponse::from)
                .orElseThrow(dev.di.chatserver.exception.ChatMessageNotFoundException::new);
    }
}
