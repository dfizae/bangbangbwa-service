package com.backend.bangbangbwa.controller;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.common.response.ApiResponse;
import com.backend.bangbangbwa.domain.favorite.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/properties")
@Tag(name = "Favorite", description = "관심 매물(찜) API")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{propertyId:\\d+}/favorites")
    @Operation(summary = "관심 매물 저장 활성화", description = "매물을 관심 매물(찜)로 저장합니다")
    public ResponseEntity<ApiResponse<Void>>
    addFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId
    ) {
        favoriteService.addFavorite(extractUserId(jwt), propertyId);

        return ResponseEntity.ok(
                ApiResponse.success("관심 매물로 저장되었습니다.")
        );
    }

    @DeleteMapping("/{propertyId:\\d+}/favorites")
    @Operation(summary = "관심 매물 저장 비활성화", description = "관심 매물(찜)에서 해제합니다")
    public ResponseEntity<ApiResponse<Void>>
    removeFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId
    ) {
        favoriteService.removeFavorite(extractUserId(jwt), propertyId);

        return ResponseEntity.ok(
                ApiResponse.success("관심 매물에서 해제되었습니다.")
        );
    }

    private Long extractUserId(Jwt jwt) {
        if (jwt == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return Long.valueOf(jwt.getSubject());
    }
}
