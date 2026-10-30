package com.backend.bangbangbwa.controller;

import com.backend.bangbangbwa.common.response.ApiResponse;
import com.backend.bangbangbwa.domain.user.dto.UserResponse;
import com.backend.bangbangbwa.domain.user.dto.UserUpdateRequest;
import com.backend.bangbangbwa.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.backend.bangbangbwa.domain.user.dto.ProfileImageResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User", description = "유저 관련 API")
public class UserController {

    private final UserService userService;

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 유저의 정보를 조회합니다")
    public ResponseEntity<ApiResponse<UserResponse>>
    getMyInfo(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = extractUserId(jwt);

        UserResponse response =
                userService.getMyInfo(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "내 정보 조회가 완료되었습니다.",
                        response
                )
        );
    }

    /**
     * 내 정보 수정
     */
    @PatchMapping("/me")
    @Operation(
            summary = "내 정보 수정",
            description = "내 정보를 수정합니다. (핸드폰 번호, 생년월일, 닉네임)"
    )
    public ResponseEntity<ApiResponse<UserResponse>>
    updateMyInfo(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        Long userId = extractUserId(jwt);

        UserResponse response =
                userService.updateMyInfo(
                        userId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "내 정보 수정이 완료되었습니다.",
                        response
                )
        );
    }

    /**
     * 프로필 이미지 수정
     */
    @PatchMapping(
            value = "/me/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "프로필 이미지 수정",
            description = "프로필 이미지를 S3에 업로드하고 사용자 프로필 이미지 URL을 수정합니다."
    )
    public ResponseEntity<ApiResponse<ProfileImageResponse>>
    updateProfileImage(
            @AuthenticationPrincipal Jwt jwt,

            @RequestPart("file")
            MultipartFile file
    ) {
        Long userId = extractUserId(jwt);

        String profileImage =
                userService.updateProfileImage(
                        userId,
                        file
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "프로필 이미지 수정이 완료되었습니다.",
                        new ProfileImageResponse(
                                profileImage
                        )
                )
        );
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 유저가 탈퇴합니다.")
    public ResponseEntity<ApiResponse<Void>>
    deleteMyAccount(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = extractUserId(jwt);

        userService.deleteMyAccount(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회원 탈퇴가 완료되었습니다."
                )
        );
    }

    private Long extractUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
