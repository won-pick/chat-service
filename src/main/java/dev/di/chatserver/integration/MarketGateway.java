package dev.di.chatserver.integration;

// 팀 API 확정 후 HTTP 어댑터를 구현합니다. 현재 ChatService에는 연결하지 않았습니다.
public interface MarketGateway {
    Product getProduct(Long productId);
    record Product(Long id, Long sellerId, String title, long price, boolean chatAllowed) {}
}
