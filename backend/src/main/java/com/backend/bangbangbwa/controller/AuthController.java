package com.backend.bangbangbwa.controller;

import com.backend.bangbangbwa.common.response.ApiResponse;
import com.backend.bangbangbwa.domain.user.dto.SocialLoginRequest;
import com.backend.bangbangbwa.domain.user.dto.TokenResponse;
import com.backend.bangbangbwa.domain.oauth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "OAuth", description = "OAuth 관련 API")
public class AuthController {

    private final AuthService authService;

    /**
     * 카카오 회원가입 및 로그인
     */
    @PostMapping("/kakao")
    @Operation(summary = "카카오 로그인", description= "카카오 계정으로 소셜로그인을 합니다.")
    public ResponseEntity<ApiResponse<TokenResponse>>
    kakaoLogin(
            @Valid @RequestBody SocialLoginRequest request
    ) {
        TokenResponse response =
                authService.loginWithKakao(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        response.isNewUser()
                                ? "카카오 회원가입이 완료되었습니다."
                                : "카카오 로그인이 완료되었습니다.",
                        response
                )
        );
    }

    /**
     * 구글 회원가입 및 로그인
     */
    @PostMapping("/google")
    @Operation(summary = "구글 로그인", description= "구글 계정으로 소셜로그인을 합니다.")
    public ResponseEntity<ApiResponse<TokenResponse>>
    googleLogin(
            @Valid @RequestBody SocialLoginRequest request
    ) {
        TokenResponse response =
                authService.loginWithGoogle(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        response.isNewUser()
                                ? "구글 회원가입이 완료되었습니다."
                                : "구글 로그인이 완료되었습니다.",
                        response
                )
        );
    }

    /**
     * 로그아웃
     *
     * Access Token만 사용하는 구조이기 때문에
     * 서버에 삭제할 토큰은 없다.
     * 응답을 받은 프론트에서 Access Token을 삭제한다.
     */
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description= "사용자가 로그아웃을 합니다.")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "로그아웃이 완료되었습니다."
                )
        );
    }
}