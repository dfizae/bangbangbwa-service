package com.backend.bangbangbwa.controller;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.common.response.ApiResponse;
import com.backend.bangbangbwa.domain.checklist.ChecklistService;
import com.backend.bangbangbwa.domain.checklist.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/checklist")
public class ChecklistController {

    private final ChecklistService checklistService;

    @GetMapping("/{meetingId}")
    @Operation(
            summary = "체크리스트 조회",
            description = "회의 참여자가 해당 회의의 체크리스트와 항목을 조회합니다.",
            tags = "Checklist - Common"
    )
    public ResponseEntity<ApiResponse<ChecklistResponse>> getChecklist(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long meetingId
    ) {
        ChecklistResponse response = checklistService.getChecklist(
                extractUserId(jwt),
                meetingId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "체크리스트 조회가 완료되었습니다.",
                        response
                )
        );
    }

    @PostMapping("/{meetingId}/items")
    @Operation(
            summary = "체크리스트 항목 추가",
            description = "회의 참여자가 체크리스트 항목을 추가합니다.",
            tags = "Checklist - Common"
    )
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> addChecklistItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long meetingId,
            @Valid @RequestBody ChecklistItemCreateRequest request
    ) {
        ChecklistItemResponse response =
                checklistService.addChecklistItem(
                        extractUserId(jwt),
                        meetingId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.created(
                                "체크리스트 항목이 추가되었습니다.",
                                response
                        )
                );
    }

    //현재는 content와 memo 모두 바뀌는 형태. 일부 수정관련해서 고민이 필요함.
    @PutMapping("/{meetingId}/items/{itemId}")
    @Operation(
            summary = "체크리스트 항목 수정",
            description = "회의 참여자가 체크리스트 항목의 내용과 메모를 수정합니다.",
            tags = "Checklist - Common"
    )
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> updateChecklistItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long meetingId,
            @PathVariable Long itemId,
            @Valid @RequestBody ChecklistItemUpdateRequest request
    ) {
        ChecklistItemResponse response =
                checklistService.updateChecklistItem(
                        extractUserId(jwt),
                        meetingId,
                        itemId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "체크리스트 항목이 수정되었습니다.",
                        response
                )
        );
    }

    @DeleteMapping("/{meetingId}/items/{itemId}")
    @Operation(
            summary = "체크리스트 항목 삭제",
            description = "회의 참여자가 체크리스트 항목을 삭제합니다.",
            tags = "Checklist - Common"
    )
    public ResponseEntity<ApiResponse<Void>> deleteChecklistItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long meetingId,
            @PathVariable Long itemId
    ) {
        checklistService.deleteChecklistItem(
                extractUserId(jwt),
                meetingId,
                itemId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "체크리스트 항목이 삭제되었습니다.",
                        null
                )
        );
    }

    @PatchMapping("/{meetingId}/items/{itemId}/status")
    @Operation(
            summary = "체크리스트 항목 상태 수정",
            description = "회의 참여자가 체크리스트 항목의 상태를 수정합니다.",
            tags = "Checklist - Common"
    )
    public ResponseEntity<ApiResponse<ChecklistItemResponse>>
    updateChecklistItemStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long meetingId,
            @PathVariable Long itemId,
            @Valid @RequestBody ChecklistItemStatusUpdateRequest request
    ) {
        ChecklistItemResponse response =
                checklistService.updateChecklistItemStatus(
                        extractUserId(jwt),
                        meetingId,
                        itemId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "체크리스트 항목 상태가 수정되었습니다.",
                        response
                )
        );
    }

    private Long extractUserId(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }
}
