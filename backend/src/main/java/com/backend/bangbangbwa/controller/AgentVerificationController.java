package com.backend.bangbangbwa.controller;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.common.response.ApiResponse;
import com.backend.bangbangbwa.domain.agent.dto.AgentVerificationResponse;
import com.backend.bangbangbwa.domain.agent.dto.LicenseCheckResponse;
import com.backend.bangbangbwa.domain.agent.service.AgentVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/agent-verifications")
@Tag(name = "중개사 인증", description = "중개사 인증")
@RequiredArgsConstructor
public class AgentVerificationController {

    private final AgentVerificationService agentVerificationService;

    @GetMapping("/license-check")
    @Operation(summary = "중개사 번호 인증", description= "중개사 번호를 기반으로 인증합니다.")
    public ApiResponse<LicenseCheckResponse> checkLicense(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String licenseNumber
    ) {
        extractUserId(jwt);

        return ApiResponse.success(
                "중개업자 등록정보 조회에 성공했습니다.",
                agentVerificationService.checkLicense(licenseNumber)
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "중개사 번호와 서류 인증", description= "중개사 번호와 서류를 신청합니다")
    public ApiResponse<AgentVerificationResponse> submit(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("licenseNumber") String licenseNumber,
            @RequestPart("document") MultipartFile document
    ) {
        Long userId = extractUserId(jwt);

        return ApiResponse.created(
                "중개사 인증 신청이 완료되었습니다.",
                agentVerificationService.submit(
                        userId,
                        licenseNumber,
                        document
                )
        );
    }

    @GetMapping("/me")
    @Operation(summary = "중개사 인증 신청 조회", description= "중개사 인증 신청 내역을 조회합니다.")
    public ApiResponse<AgentVerificationResponse> getMine(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponse.success(
                "내 중개사 인증 신청 조회에 성공했습니다.",
                agentVerificationService.getMine(extractUserId(jwt))
        );
    }

    private Long extractUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }
}
