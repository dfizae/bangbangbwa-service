package com.backend.bangbangbwa.controller;

import com.backend.bangbangbwa.common.response.ApiResponse;
import com.backend.bangbangbwa.domain.agent.dto.AgentVerificationDetailResponse;
import com.backend.bangbangbwa.domain.agent.dto.AgentVerificationResponse;
import com.backend.bangbangbwa.domain.agent.dto.AgentVerificationReviewRequest;
import com.backend.bangbangbwa.domain.agent.entity.AgentVerificationStatus;
import com.backend.bangbangbwa.domain.agent.service.AgentVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/agent-verifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "중개사 인증-admin", description = "중개사 인증-admin")
public class AdminAgentVerificationController {

    private final AgentVerificationService agentVerificationService;

    @GetMapping
    @Operation(summary = "중개사 인증 신청 조회", description= "관리자가 받은 중개사 인증 신청을 모두 조회합니다.")
    public ApiResponse<Page<AgentVerificationResponse>> getAll(
            @RequestParam(required = false)
            AgentVerificationStatus status,
            @ParameterObject Pageable pageable
    ) {
        return ApiResponse.success(
                "중개사 인증 요청 목록 조회에 성공했습니다.",
                agentVerificationService.getAll(status, pageable)
        );
    }

    @GetMapping("/{verificationId}")
    @Operation(summary = "중개사 인증 신청 상세 조회", description= "중개사 인증 신청 상세 내역을 조회합니다.")
    public ApiResponse<AgentVerificationDetailResponse> getDetail(
            @PathVariable Long verificationId
    ) {
        return ApiResponse.success(
                "중개사 인증 요청 상세 조회에 성공했습니다.",
                agentVerificationService.getDetail(verificationId)
        );
    }

    @PatchMapping("/{verificationId}/review")
    @Operation(summary = "중개사 인증 신청 승인/반려", description= "중개사 인증 신청을 승인/반려 합니다.")
    public ApiResponse<AgentVerificationResponse> review(
            @PathVariable Long verificationId,
            @Valid @RequestBody AgentVerificationReviewRequest request
    ) {
        return ApiResponse.success(
                "중개사 인증 요청 심사가 완료되었습니다.",
                agentVerificationService.review(
                        verificationId,
                        request.decision()
                )
        );
    }
}
