package dev.di.chatserver.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateChatRoomRequest(

        @NotNull(message = "상품 번호는 필수입니다.")
        @Positive(message = "상품 번호는 1 이상이어야 합니다.")
        Long productId,

        @NotNull(message = "구매자 번호는 필수입니다.")
        @Positive(message = "구매자 번호는 1 이상이어야 합니다.")
        Long buyerId,

        @NotNull(message = "판매자 번호는 필수입니다.")
        @Positive(message = "판매자 번호는 1 이상이어야 합니다.")
        Long sellerId
) {
}