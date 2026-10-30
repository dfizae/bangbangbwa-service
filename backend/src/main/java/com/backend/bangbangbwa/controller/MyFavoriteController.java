package com.backend.bangbangbwa.controller;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.common.response.ApiResponse;
import com.backend.bangbangbwa.domain.favorite.service.FavoriteService;
import com.backend.bangbangbwa.domain.property.dto.PropertySummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
@Tag(name = "Favorite", description = "관심 매물(찜) API")
public class MyFavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/favorite-properties")
    @Operation(summary = "저장한 매물 조회", description = "로그인한 사용자가 저장한 관심 매물 목록을 조회합니다")
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>>
    getMyFavorites(
            @AuthenticationPrincipal Jwt jwt,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<PropertySummaryResponse> response =
                favoriteService.getMyFavorites(extractUserId(jwt), pageable);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "저장한 매물 조회가 완료되었습니다.",
                        response
                )
        );
    }

    private Long extractUserId(Jwt jwt) {
        if (jwt == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return Long.valueOf(jwt.getSubject());
    }
}
