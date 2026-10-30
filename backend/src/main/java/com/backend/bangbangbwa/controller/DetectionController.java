package com.backend.bangbangbwa.controller;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.common.response.ApiResponse;
import com.backend.bangbangbwa.domain.detection.DetectionService;
import com.backend.bangbangbwa.domain.detection.dto.SessionCaptureBatchCreateRequest;
import com.backend.bangbangbwa.domain.detection.dto.SessionCaptureDeleteRequest;
import com.backend.bangbangbwa.domain.detection.dto.AiCaptureSaveResult;
import com.backend.bangbangbwa.domain.detection.dto.SessionCaptureDefectResponse;
import com.backend.bangbangbwa.domain.detection.dto.SessionCaptureResponse;
import com.backend.bangbangbwa.domain.detection.dto.DefectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;
import com.backend.bangbangbwa.security.AiServerTokenAuthenticationFilter;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions/{sessionId}/captures")
@Tag(name = "Detection", description = "세션 캡처 및 하자 탐지 API")
public class DetectionController {

    private final DetectionService detectionService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "세션 캡처 저장",
            description = "세입자가 선택한 이미지와 탐지 결과를 저장합니다."
    )
    public ResponseEntity<ApiResponse<List<SessionCaptureDefectResponse>>>
    createCaptures(
            Authentication authentication,
            @PathVariable Long sessionId,
            @RequestPart(value = "images", required = false)
            List<MultipartFile> images,
            @Parameter(
                    description = """
                            이미지 순서에 맞춘 JSON 문자열입니다.<br>
                            이미지 1장: `{"captures":[{}]}`<br>
                            이미지 2장: `{"captures":[{},{}]}`<br>
                            bbox는 YOLO 형식의 0~1 정규화 좌표
                            `(x 중심, y 중심, 너비, 높이)`입니다.<br>
                            탐지 결과 예시:
                            `{"captures":[{"defects":[{"type":"MOLD","confidence":0.92,"description":"창틀 곰팡이 의심","bboxXCenter":0.5,"bboxYCenter":0.4,"bboxWidth":0.25,"bboxHeight":0.3}]}]}`
                            """,
                    schema = @Schema(
                            type = "string",
                            example = "{\"captures\":[{\"defects\":[{\"type\":\"MOLD\",\"confidence\":0.92,\"description\":\"창틀 곰팡이 의심\",\"bboxXCenter\":0.5,\"bboxYCenter\":0.4,\"bboxWidth\":0.25,\"bboxHeight\":0.3}]}]}"
                    )
            )
            @RequestPart("metadata") String metadata
    ) {
        SessionCaptureBatchCreateRequest request =
                parseAndValidateMetadata(metadata);
        List<MultipartFile> normalizedImages =
                images == null ? List.of() : images;

        if (hasAiServerAuthority(authentication)) {
            AiCaptureSaveResult result = detectionService.createAiCaptures(
                    sessionId,
                    normalizedImages,
                    request
            );
            if (!result.created()) {
                return ResponseEntity.ok(
                        ApiResponse.success(
                                "이미 처리된 AI 캡처 요청입니다.",
                                result.captures()
                        )
                );
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.created(
                            "세션 캡처를 저장했습니다.",
                            result.captures()
                    )
            );
        }

        List<SessionCaptureDefectResponse> response =
                detectionService.createCaptures(
                        extractUserId(authentication),
                        sessionId,
                        normalizedImages,
                        request
                );
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created(
                        "세션 캡처를 저장했습니다.",
                        response
                )
        );
    }

    @PostMapping(
            value = "/user",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "사용자 캡처 저장",
            description = "세입자가 캡처한 이미지를 하자 탐지 없이 S3에 저장합니다."
    )
    public ResponseEntity<ApiResponse<SessionCaptureResponse>>
    createUserCapture(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long sessionId,
            @RequestPart("image") MultipartFile image
    ) {
        SessionCaptureResponse response =
                detectionService.createUserCapture(
                        extractUserId(jwt),
                        sessionId,
                        image
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created(
                        "사용자 캡처를 저장했습니다.",
                        response
                )
        );
    }

    @GetMapping
    @Operation(
            summary = "세션 캡처 목록 조회",
            description = "세입자가 해당 세션에 저장한 이미지 목록을 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<SessionCaptureResponse>>>
    getCaptures(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long sessionId
    ) {
        List<SessionCaptureResponse> response =
                detectionService.getCaptures(
                        extractUserId(jwt),
                        sessionId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "세션 캡처 목록을 조회했습니다.",
                        response
                )
        );
    }

    @GetMapping("/defects")
    @Operation(
            summary = "하자 검출 캡처 목록 조회",
            description = "AI 모델이 하자를 검출한 캡처 이미지만 추려서 조회합니다. "
                    + "오검출 확인 및 삭제 대상 선별에 사용합니다."
    )
    public ResponseEntity<ApiResponse<List<SessionCaptureDefectResponse>>>
    getDefectCaptures(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long sessionId
    ) {
        List<SessionCaptureDefectResponse> response =
                detectionService.getDefectCaptures(
                        extractUserId(jwt),
                        sessionId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "하자가 검출된 캡처 목록을 조회했습니다.",
                        response
                )
        );
    }

    @GetMapping("/{captureId}/defects")
    @Operation(
            summary = "캡처 하자 목록 조회",
            description = "세션 캡처 이미지에서 탐지된 하자 목록을 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<DefectResponse>>>
    getDefects(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long sessionId,
            @PathVariable Long captureId
    ) {
        List<DefectResponse> response =
                detectionService.getDefects(
                        extractUserId(jwt),
                        sessionId,
                        captureId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "캡처 하자 목록을 조회했습니다.",
                        response
                )
        );
    }

    @DeleteMapping
    @Operation(
            summary = "세션 캡처 일괄 삭제",
            description = "오검출 등으로 불필요한 캡처 이미지를 선택해 한 번에 삭제합니다. "
                    + "요청 본문의 captureIds 배열에 하나 또는 여러 개의 캡처 ID를 담아 보냅니다. "
                    + "요청한 캡처가 하나라도 해당 세션에 없으면 삭제하지 않고 실패합니다. "
                    + "저장된 이미지와 연관된 하자 정보가 함께 제거됩니다."
    )
    public ResponseEntity<ApiResponse<Void>>
    deleteCaptures(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionCaptureDeleteRequest request
    ) {
        detectionService.deleteCaptures(
                extractUserId(jwt),
                sessionId,
                request.getCaptureIds()
        );

        return ResponseEntity.ok(
                ApiResponse.success("선택한 세션 캡처를 삭제했습니다.")
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

    private Long extractUserId(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return extractUserId(jwt);
    }

    private boolean hasAiServerAuthority(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream().anyMatch(
                        authority -> AiServerTokenAuthenticationFilter
                                .AI_SERVER_AUTHORITY
                                .equals(authority.getAuthority())
                );
    }

    private SessionCaptureBatchCreateRequest parseAndValidateMetadata(
            String metadata
    ) {
        try {
            SessionCaptureBatchCreateRequest request =
                    objectMapper.readValue(
                            metadata,
                            SessionCaptureBatchCreateRequest.class
                    );

            Set<ConstraintViolation<SessionCaptureBatchCreateRequest>>
                    violations = validator.validate(request);
            if (!violations.isEmpty()) {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }

            return request;
        } catch (CustomException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }
}
