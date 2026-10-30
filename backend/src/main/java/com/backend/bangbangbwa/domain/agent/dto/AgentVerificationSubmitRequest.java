package com.backend.bangbangbwa.domain.agent.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

public record AgentVerificationSubmitRequest(
        @NotBlank(message = "중개업자 등록번호는 필수입니다.")
        String licenseNumber,

        MultipartFile document
) {
}
