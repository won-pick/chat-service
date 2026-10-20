package dev.di.chatserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// 인증 연동 전의 테스트용 발신자 번호입니다.
public record SendMessageRequest(
        @NotNull(message = "발신자 번호는 필수입니다.")
        @Positive(message = "발신자 번호는 1 이상이어야 합니다.")
        Long senderId,

        @NotBlank(message = "메시지 내용은 필수입니다.")
        @Size(max = 1000, message = "메시지는 1000자 이하여야 합니다.")
        String content
) {
}
