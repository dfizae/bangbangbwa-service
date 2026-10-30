package com.backend.bangbangbwa.domain.agent.dto;

import jakarta.validation.constraints.NotNull;

public record AgentVerificationReviewRequest(
        @NotNull(message = "심사 결과는 필수입니다.")
        VerificationDecision decision
) {
    public enum VerificationDecision {
        APPROVED,
        REJECTED
    }
}
