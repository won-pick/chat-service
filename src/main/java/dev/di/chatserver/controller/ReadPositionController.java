package dev.di.chatserver.controller;

import dev.di.chatserver.dto.ReadPositionResponse;
import dev.di.chatserver.dto.UpdateReadPositionRequest;
import dev.di.chatserver.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat/rooms/{roomId}/read-position")
public class ReadPositionController {
    private final ChatService chatService;

    public ReadPositionController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PatchMapping
    public ReadPositionResponse update(
            @PathVariable("roomId") @Positive Long roomId,
            @Valid @RequestBody UpdateReadPositionRequest request) {
        return chatService.updateReadPosition(
                roomId, request.memberId(), request.lastReadMessageId());
    }

    @GetMapping
    public ReadPositionResponse get(
            @PathVariable("roomId") @Positive Long roomId,
            @RequestParam("memberId") @Positive Long memberId) {
        return chatService.getReadPosition(roomId, memberId);
    }
    @GetMapping("/peer")
    public dev.di.chatserver.dto.PeerReadResponse getPeer(
            @PathVariable("roomId") @Positive Long roomId,
            @RequestParam("memberId") @Positive Long memberId) {
        return chatService.getPeerReadPosition(roomId, memberId);
    }
}
