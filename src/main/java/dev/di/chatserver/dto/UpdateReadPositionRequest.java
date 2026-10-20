package dev.di.chatserver.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// 인증 연동 전 테스트용 memberId입니다.
public record UpdateReadPositionRequest(
        @NotNull(message = "회원 번호는 필수입니다.")
        @Positive(message = "회원 번호는 1 이상이어야 합니다.") Long memberId,
        @NotNull(message = "읽은 메시지 번호는 필수입니다.")
        @Positive(message = "메시지 번호는 1 이상이어야 합니다.") Long lastReadMessageId
) {}
