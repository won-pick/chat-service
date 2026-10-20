package dev.di.chatserver.controller;
import java.util.List;
import dev.di.chatserver.domain.ChatRoom;
import dev.di.chatserver.dto.ChatRoomResponse;
import dev.di.chatserver.dto.ChatRoomListResponse;
import dev.di.chatserver.dto.CreateChatRoomRequest;
import dev.di.chatserver.service.ChatService;
import dev.di.chatserver.service.CreateChatRoomResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/chat/rooms")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatRoomResponse> createRoom(
            @Valid @RequestBody CreateChatRoomRequest request) {

        CreateChatRoomResult result = chatService.createRoom(
                request.productId(),
                request.buyerId(),
                request.sellerId()
        );

        ChatRoomResponse response =
                ChatRoomResponse.from(result.room());

        // 새로 만든 방이면 201
        if (result.created()) {
            return ResponseEntity
                    .created(URI.create(
                            "/chat/rooms/" + result.room().getId()))
                    .body(response);
        }

        // 기존 방이면 200
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}")
    public ChatRoomResponse getRoom(
            @PathVariable("roomId") @Positive Long roomId,
            @RequestParam("memberId") @Positive Long memberId) {

        ChatRoom room = chatService.getRoomForMember(roomId, memberId);
        return ChatRoomResponse.from(room);
    }
    @GetMapping
    public List<ChatRoomListResponse> getMyRooms(
            @RequestParam("memberId") @Positive Long memberId) {

        return chatService.getMyRoomSummaries(memberId);
    }
}