package com.backend.bangbangbwa.controller;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.common.response.ApiResponse;
import com.backend.bangbangbwa.domain.session.dto.SessionCreateResponse;
import com.backend.bangbangbwa.domain.session.dto.SessionEndResponse;
import com.backend.bangbangbwa.domain.session.dto.SessionJoinResponse;
import com.backend.bangbangbwa.domain.session.dto.SessionLeaveResponse;
import com.backend.bangbangbwa.domain.session.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Session", description = "세션 관련 API")
public class SessionController {

    private final SessionService sessionService;

    /**
     * RTC 세션 생성
     */
    @PostMapping("/meetings/{meetingId}/session")
    @Operation(summary = "세션 생성", description = "세션을 생성합니다.")
    public ResponseEntity<ApiResponse<SessionCreateResponse>> createSession(
            @PathVariable("meetingId") Long meetingId,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);

        SessionCreateResponse response =
                sessionService.createSession(
                        meetingId,
                        userId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.created(
                                "RTC 세션이 생성되었습니다.",
                                response
                        )
                );
    }

    /**
     * RTC 세션 입장
     */
    @PostMapping("/sessions/{sessionId}/join")
    @Operation(summary = "세션 입장", description = "세션을 입장합니다.")
    public ResponseEntity<ApiResponse<SessionJoinResponse>> joinSession(
            @PathVariable("sessionId") Long sessionId,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);

        SessionJoinResponse response =
                sessionService.joinSession(
                        sessionId,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "RTC 세션에 입장했습니다.",
                        response
                )
        );
    }

    /**
     * RTC 세션 퇴장
     */
    @PostMapping("/sessions/{sessionId}/leave")
    @Operation(summary = "세션 퇴장", description = "세션을 퇴장합니다.")
    public ResponseEntity<ApiResponse<SessionLeaveResponse>> leaveSession(
            @PathVariable("sessionId") Long sessionId,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);

        SessionLeaveResponse response =
                sessionService.leaveSession(
                        sessionId,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "RTC 세션에서 퇴장했습니다.",
                        response
                )
        );
    }

    /**
     * RTC 세션 종료
     */
    @PatchMapping("/sessions/{sessionId}/end")
    @Operation(summary = "세션 종료", description = "세션을 종료합니다.")
    public ResponseEntity<ApiResponse<SessionEndResponse>> endSession(
            @PathVariable("sessionId") Long sessionId,
            Authentication authentication
    ) {
        Long userId = getCurrentUserId(authentication);

        SessionEndResponse response =
                sessionService.endSession(
                        sessionId,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "RTC 세션이 종료되었습니다.",
                        response
                )
        );
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }
}