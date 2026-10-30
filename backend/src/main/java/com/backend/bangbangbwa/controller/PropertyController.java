package com.backend.bangbangbwa.controller;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.common.response.ApiResponse;
import com.backend.bangbangbwa.domain.property.dto.FilterOptionResponse;
import com.backend.bangbangbwa.domain.property.dto.PropertyCreateRequest;
import com.backend.bangbangbwa.domain.property.dto.PropertyDetailResponse;
import com.backend.bangbangbwa.domain.property.dto.PropertyImageResponse;
import com.backend.bangbangbwa.domain.property.dto.PropertyMapResponse;
import com.backend.bangbangbwa.domain.property.dto.PropertySearchRequest;
import com.backend.bangbangbwa.domain.property.dto.PropertyStatusUpdateRequest;
import com.backend.bangbangbwa.domain.property.dto.PropertySummaryResponse;
import com.backend.bangbangbwa.domain.property.dto.PropertyUpdateRequest;
import com.backend.bangbangbwa.domain.property.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/properties")
@Tag(name = "Property", description = "매물 관련 API")
public class PropertyController {

    private final PropertyService propertyService;

    /**
     * 매물 등록
     */
    @PostMapping
    @Operation(summary = "매물 등록", description = "중개사가 새 매물을 등록합니다")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>>
    createProperty(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PropertyCreateRequest request
    ) {
        Long userId = extractUserId(jwt);

        PropertyDetailResponse response =
                propertyService.createProperty(
                        userId,
                        request
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created(
                        "매물 등록이 완료되었습니다.",
                        response
                )
        );
    }

    @PatchMapping("/{propertyId:\\d+}")
    @Operation(summary = "매물 수정", description = "중개사가 자신이 등록한 매물 정보를 수정합니다")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>>
    updateProperty(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId,
            @Valid @RequestBody PropertyUpdateRequest request
    ) {
        Long userId = extractUserId(jwt);

        PropertyDetailResponse response =
                propertyService.updateProperty(
                        propertyId,
                        userId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "매물 수정이 완료되었습니다.",
                        response
                )
        );
    }

    @PatchMapping("/{propertyId:\\d+}/status")
    @Operation(summary = "매물 거래 상태 변경", description = "중개사가 자신이 등록한 매물의 거래 상태를 변경합니다")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>>
    changeStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId,
            @Valid @RequestBody PropertyStatusUpdateRequest request
    ) {
        Long userId = extractUserId(jwt);

        PropertyDetailResponse response =
                propertyService.changeStatus(
                        propertyId,
                        userId,
                        request.getStatus()
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "매물 거래 상태가 변경되었습니다.",
                        response
                )
        );
    }

    @DeleteMapping("/{propertyId:\\d+}")
    @Operation(summary = "매물 삭제", description = "중개사가 자신이 등록한 매물을 삭제합니다")
    public ResponseEntity<ApiResponse<Void>>
    deleteProperty(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId
    ) {
        propertyService.deleteProperty(
                propertyId,
                extractUserId(jwt)
        );

        return ResponseEntity.ok(
                ApiResponse.success("매물 삭제가 완료되었습니다.")
        );
    }

    @PostMapping(
            value = "/{propertyId:\\d+}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "매물 이미지 업로드", description = "중개사가 매물에 이미지를 업로드합니다")
    public ResponseEntity<ApiResponse<List<PropertyImageResponse>>>
    uploadImages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId,
            @RequestPart("images") List<MultipartFile> images
    ) {
        Long userId = extractUserId(jwt);

        List<PropertyImageResponse> response =
                propertyService.uploadImages(
                        propertyId,
                        userId,
                        images
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created(
                        "매물 이미지가 업로드되었습니다.",
                        response
                )
        );
    }

    @DeleteMapping("/{propertyId:\\d+}/images/{imageId:\\d+}")
    @Operation(summary = "매물 이미지 삭제", description = "중개사가 매물 이미지를 삭제합니다")
    public ResponseEntity<ApiResponse<Void>>
    deleteImage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId,
            @PathVariable Long imageId
    ) {
        propertyService.deleteImage(
                propertyId,
                imageId,
                extractUserId(jwt)
        );

        return ResponseEntity.ok(
                ApiResponse.success("매물 이미지가 삭제되었습니다.")
        );
    }

    /**
     * 매물 목록 조회
     */
    @GetMapping
    @Operation(summary = "매물 목록 조회", description = "조건에 맞는 매물 목록을 조회합니다")
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>>
    getProperties(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sigungu,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) Integer minDeposit,
            @RequestParam(required = false) Integer maxDeposit,
            @ParameterObject @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        PropertySearchRequest request = new PropertySearchRequest(
                query,
                sigungu,
                transactionType,
                roomType,
                minDeposit,
                maxDeposit
        );

        Page<PropertySummaryResponse> response =
                propertyService.getProperties(
                        request,
                        pageable,
                        extractUserIdOrNull(jwt)
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "매물 목록 조회가 완료되었습니다.",
                        response
                )
        );
    }

    /**
     * 내 매물 목록 조회
     */
    @GetMapping("/me")
    @Operation(summary = "내 매물 목록 조회", description = "로그인한 중개사가 등록한 매물 목록을 조회합니다")
    public ResponseEntity<ApiResponse<Page<PropertySummaryResponse>>>
    getMyProperties(
            @AuthenticationPrincipal Jwt jwt,
            @ParameterObject @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        if (jwt == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = extractUserId(jwt);

        Page<PropertySummaryResponse> response =
                propertyService.getMyProperties(
                        userId,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "내 매물 목록 조회가 완료되었습니다.",
                        response
                )
        );
    }

    /**
     * 매물 지도 조회
     */
    @GetMapping("/map")
    @Operation(summary = "매물 지도 조회", description = "지도 영역(남서·북동 좌표) 안의 매물을 조회합니다")
    public ResponseEntity<ApiResponse<List<PropertyMapResponse>>>
    getPropertiesInBounds(
            @RequestParam BigDecimal swLat,
            @RequestParam BigDecimal swLng,
            @RequestParam BigDecimal neLat,
            @RequestParam BigDecimal neLng
    ) {
        List<PropertyMapResponse> response =
                propertyService.getPropertiesInBounds(
                        swLat,
                        swLng,
                        neLat,
                        neLng
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "매물 지도 조회가 완료되었습니다.",
                        response
                )
        );
    }

    /**
     * 매물 상세 조회
     */
    @GetMapping("/{propertyId:\\d+}")
    @Operation(summary = "매물 상세 조회", description = "매물 상세 정보를 조회합니다")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>>
    getProperty(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId
    ) {
        PropertyDetailResponse response =
                propertyService.getProperty(propertyId, extractUserIdOrNull(jwt));

        return ResponseEntity.ok(
                ApiResponse.success(
                        "매물 상세 조회가 완료되었습니다.",
                        response
                )
        );
    }

    @GetMapping("/filters")
    @Operation(summary = "매물 필터 옵션 조회", description = "매물 검색 필터에 사용할 옵션 목록을 조회합니다")
    public ResponseEntity<ApiResponse<FilterOptionResponse>>
    getFilterOptions() {
        FilterOptionResponse response = propertyService.getFilterOptions();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "매물 필터 옵션 조회가 완료되었습니다.",
                        response
                )
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

    private Long extractUserIdOrNull(Jwt jwt) {
        return jwt != null ? Long.valueOf(jwt.getSubject()) : null;
    }
}
