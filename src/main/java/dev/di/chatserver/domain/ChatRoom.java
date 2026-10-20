package dev.di.chatserver.domain;

public class ChatRoom {

    private final Long id;
    private final Long productId;
    private final Long buyerId;
    private final Long sellerId;

    public ChatRoom(Long id, Long productId,
                    Long buyerId, Long sellerId) {
        this.id = id;
        this.productId = productId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public Long getSellerId() {
        return sellerId;
    }
}