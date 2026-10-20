package dev.di.chatserver.service;

import dev.di.chatserver.domain.ChatMessage;
import dev.di.chatserver.domain.ChatRoom;
import dev.di.chatserver.exception.ChatAccessDeniedException;
import dev.di.chatserver.exception.ChatRoomNotFoundException;
import dev.di.chatserver.exception.SelfChatNotAllowedException;
import dev.di.chatserver.repository.ChatMessageStore;
import dev.di.chatserver.repository.ChatRoomStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Comparator;
import dev.di.chatserver.exception.ChatRequestException;
import dev.di.chatserver.exception.ErrorCode;
import dev.di.chatserver.dto.ChatRoomListResponse;
import dev.di.chatserver.dto.ChatMessageResponse;
import dev.di.chatserver.repository.ReadPositionStore;
import dev.di.chatserver.dto.ReadPositionResponse;
import dev.di.chatserver.dto.PeerReadResponse;
import dev.di.chatserver.exception.ChatMessageNotFoundException;

@Service
public class ChatService {

    private final ChatRoomStore chatRoomRepository;
    private final ChatMessageStore chatMessageRepository;
    private final ReadPositionStore readPositionRepository;
    private final MessageDeduplicator idempotencyService;

    public ChatService(
            ChatRoomStore chatRoomRepository,
            ChatMessageStore chatMessageRepository,
            ReadPositionStore readPositionRepository,
            MessageDeduplicator idempotencyService) {

        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.readPositionRepository = readPositionRepository;
        this.idempotencyService = idempotencyService;
    }

    // 채팅방 생성 또는 기존 방 반환
    public synchronized CreateChatRoomResult createRoom(
            Long productId, Long buyerId, Long sellerId) {

        if (productId == null || buyerId == null || sellerId == null) {
            throw new IllegalArgumentException(
                    "상품 번호와 회원 번호가 필요합니다.");
        }

        if (buyerId.equals(sellerId)) {
            throw new SelfChatNotAllowedException();
        }

        var existingRoom =
                chatRoomRepository.findByProductIdAndBuyerIdAndSellerId(
                        productId, buyerId, sellerId);

        if (existingRoom.isPresent()) {
            return new CreateChatRoomResult(existingRoom.get(), false);
        }

        ChatRoom newRoom =
                chatRoomRepository.save(productId, buyerId, sellerId);

        return new CreateChatRoomResult(newRoom, true);
    }

    // 채팅방 상세 조회
    public ChatRoom getRoom(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() ->
                        new ChatRoomNotFoundException(roomId));
    }

    // 내가 참여한 채팅방 목록 조회
    public List<ChatRoom> getMyRooms(Long memberId) {
        return chatRoomRepository.findAllByMemberId(memberId);
    }

    // 메시지 전송
    public ChatMessage sendMessage(
            Long roomId, Long senderId, String content, String key) {

        ChatRoom room = getRoom(roomId);
        validateParticipant(room, senderId);

        if (content == null || content.isBlank() || content.length() > 1000) {
            throw new ChatRequestException(400, ErrorCode.INVALID_INPUT, "메시지는 공백 없이 1~1000자로 입력해 주세요.");
        }
        return idempotencyService.execute(roomId, senderId, key, content,
                () -> chatMessageRepository.save(roomId, senderId, content));
    }

    // 채팅방의 메시지 목록 조회
    public List<ChatMessage> getMessages(
            Long roomId, Long memberId) {

        ChatRoom room = getRoom(roomId);
        validateParticipant(room, memberId);

        return chatMessageRepository.findAllByRoomId(roomId);
    }

    public ChatRoom getRoomForMember(Long roomId, Long memberId) {
        ChatRoom room = getRoom(roomId);
        validateParticipant(room, memberId);
        return room;
    }

    public List<ChatMessage> getMessagePage(Long roomId, Long memberId,
                                          Long afterId, Long beforeId, int limit) {
        if (afterId != null && beforeId != null) {
            throw new ChatRequestException(400, ErrorCode.INVALID_INPUT,
                    "afterId와 beforeId는 동시에 사용할 수 없습니다.");
        }
        var messages = getMessages(roomId, memberId);
        if (afterId != null) {
            return messages.stream().filter(m -> m.getId() > afterId).limit(limit).toList();
        }
        var filtered = messages.stream()
                .filter(m -> beforeId == null || m.getId() < beforeId).toList();
        return filtered.subList(Math.max(0, filtered.size() - limit), filtered.size());
    }

    public PeerReadResponse getPeerReadPosition(Long roomId, Long memberId) {
        ChatRoom room = getRoomForMember(roomId, memberId);
        Long peerId = room.getBuyerId().equals(memberId) ? room.getSellerId() : room.getBuyerId();
        return new PeerReadResponse(peerId, readPositionRepository.find(roomId, peerId));
    }

    // 채팅방 참여자인지 확인
    private void validateParticipant(ChatRoom room, Long memberId) {

        boolean isParticipant =
                room.getBuyerId().equals(memberId)
                        || room.getSellerId().equals(memberId);

        if (!isParticipant) {
            throw new ChatAccessDeniedException();
        }
    }
    public ReadPositionResponse updateReadPosition(
            Long roomId, Long memberId, Long messageId) {
        ChatRoom room = getRoom(roomId);
        validateParticipant(room, memberId);
        // 다른 방의 메시지도 이 방에서 찾을 수 없다고 응답합니다.
        chatMessageRepository.findById(messageId)
                .filter(message -> message.getRoomId().equals(roomId))
                .orElseThrow(ChatMessageNotFoundException::new);
        Long position = readPositionRepository.advance(roomId, memberId, messageId);
        return new ReadPositionResponse(roomId, memberId, position);
    }

    public ReadPositionResponse getReadPosition(Long roomId, Long memberId) {
        validateParticipant(getRoom(roomId), memberId);
        return new ReadPositionResponse(roomId, memberId,
                readPositionRepository.find(roomId, memberId));
    }
    // 내 메시지는 제외하고, 읽은 위치보다 뒤에 있는 상대 메시지만 셉니다.
    public List<ChatRoomListResponse> getMyRoomSummaries(Long memberId) {
        return getMyRooms(memberId).stream()
                .map(room -> {
                    var messages = chatMessageRepository.findAllByRoomId(room.getId());
                    Long readPosition = readPositionRepository.find(room.getId(), memberId);
                    long unreadCount = messages.stream()
                            .filter(message -> !message.getSenderId().equals(memberId))
                            .filter(message -> message.getId() > readPosition)
                            .count();
                    ChatMessageResponse lastMessage = messages.isEmpty() ? null
                            : ChatMessageResponse.from(messages.get(messages.size() - 1));
                    return new ChatRoomListResponse(room.getId(), room.getProductId(),
                            room.getBuyerId(), room.getSellerId(), lastMessage, unreadCount);
                })
                .sorted(Comparator.comparingLong((ChatRoomListResponse room) ->
                        room.lastMessage() == null ? 0L : room.lastMessage().id())
                        .reversed().thenComparing(ChatRoomListResponse::id, Comparator.reverseOrder()))
                .toList();
    }
}
