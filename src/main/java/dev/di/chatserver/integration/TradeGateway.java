package dev.di.chatserver.integration;

// 요청 URL·인증·상태 계약이 확정되면 구현할 연동 경계입니다.
public interface TradeGateway {
    Trade requestPayment(Long productId, Long buyerId, Long sellerId, String idempotencyKey);
    Trade getTrade(Long tradeId);
    record Trade(Long id, Long productId, long amount, String status) {}
}
