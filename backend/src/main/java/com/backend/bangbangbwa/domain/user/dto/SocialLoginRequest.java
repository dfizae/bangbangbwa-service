package com.backend.bangbangbwa.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SocialLoginRequest {

        @NotBlank(message = "소셜 로그인 Authorization Code는 필수입니다.")
        private String authorizationCode;
}