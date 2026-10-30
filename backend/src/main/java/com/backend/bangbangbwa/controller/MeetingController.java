package com.backend.bangbangbwa.controller;

import com.backend.bangbangbwa.common.response.ApiResponse;
import com.backend.bangbangbwa.domain.meeting.dto.MeetingCreateRequest;
import com.backend.bangbangbwa.domain.meeting.dto.MeetingPreferredTimesUpdateRequest;
import com.backend.bangbangbwa.domain.meeting.dto.MeetingResponse;
import com.backend.bangbangbwa.domain.meeting.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api")
public class MeetingController {

    private final MeetingService meetingService;


    //임차인이 특정 매물의 회의 요청을 보냄
    @PostMapping("/properties/{propertyId}/meetings")
    @Operation(summary = "회의 가능 시간 등록", tags = "Meeting - Tenant")
    public ResponseEntity<ApiResponse<MeetingResponse>> createMeetings(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId,
            @Valid @RequestBody MeetingCreateRequest request
    ) {
        MeetingResponse response =
                meetingService.createMeetings(
                        extractUserId(jwt),
                        propertyId,
                        request
                );

        return ResponseEntity.status(201)
                .body(
                        ApiResponse.created(
                                "회의 가능 시간이 등록되었습니다.",
                                response
                        )
                );
    }

    //해당 매물에 대해서 다른 임차인이 이미 회의를 확정한 시간대라면 신청 불가능하도록 함.
    @GetMapping("/properties/{propertyId}/meetings/unavailable-times")
    @Operation(summary = "매물별 회의 등록 불가능 시간 조회", tags = "Meeting - Tenant")
    public ResponseEntity<ApiResponse<List<LocalDateTime>>> getUnavailableMeetingTimes(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {

        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay();

        List<LocalDateTime> response =
                meetingService.getUnavailableMeetingTimes(
                        extractUserId(jwt),
                        propertyId,
                        fromDateTime,
                        toDateTime
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회의 등록 불가능 시간 조회가 완료되었습니다.",
                        response
                )
        );
    }

    //중개사가 매물의 신청된 회의들을 볼 수 있음
    @GetMapping("/properties/{propertyId}/meetings/request")
    @Operation(summary = "매물 별 회의 요청 리스트 조회", tags = "Meeting - Agent")
    public ResponseEntity<ApiResponse<Page<MeetingResponse>>> getRequestedMeetings(
            @PathVariable Long propertyId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction
    ) {
        Page<MeetingResponse> response =
                meetingService.getRequestedMeetings(
                        propertyId,
                        extractUserId(jwt),
                        PageRequest.of(page, size, direction, "updatedAt")
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "예약 가능한 회의 목록 조회가 완료되었습니다.",
                        response
                )
        );
    }

    //중개사가 본인이 확정한 매물 회의를 조회 가능
    @GetMapping("/properties/{propertyId}/meetings")
    @Operation(summary = "공인중개사용 매물 회의 일정 조회", tags = "Meeting - Agent")
    public ResponseEntity<ApiResponse<Page<MeetingResponse>>> getPropertyMeetingsForAgent(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long propertyId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction
    ) {
        Page<MeetingResponse> response =
                meetingService.getPropertyMeetingsForAgent(
                        extractUserId(jwt),
                        propertyId,
                        PageRequest.of(page, size, direction, "scheduledAt")
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "매물 회의 일정 조회가 완료되었습니다.",
                        response
                )
        );
    }

    //정책적인 회의가 필요함.
//    @PatchMapping("/meetings/{meetingId}/request")
//    @Operation(summary = "회의 예약 요청")
//    public ResponseEntity<ApiResponse<MeetingResponse>> requestMeeting(
//            @AuthenticationPrincipal Jwt jwt,
//            @PathVariable Long meetingId
//    ) {
//        MeetingResponse response =
//                meetingService.requestMeeting(
//                        extractUserId(jwt),
//                        meetingId
//                );
//
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        "회의 예약을 요청했습니다.",
//                        response
//                )
//        );
//    }

    //중개인이 임차인의 요청을 받아드림.
    @PatchMapping("/meetings/{meetingId}/confirm")
    @Operation(summary = "회의 예약 확정", tags = "Meeting - Agent")
    public ResponseEntity<ApiResponse<MeetingResponse>> confirmMeeting(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long meetingId,
            @RequestParam int preferredRank
    ) {
        MeetingResponse response =
                meetingService.confirmMeeting(
                        extractUserId(jwt),
                        meetingId,
                        preferredRank
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회의 예약이 확정되었습니다.",
                        response
                )
        );
    }

    //중개인이 임차인의 회의 예약을 거절 -> 어짜피 중개인이 회의 예약을 안받아주면 되므로 필요 없을 것 같음
    @PatchMapping("/meetings/{meetingId}/reject")
    @Operation(summary = "회의 예약 요청 거절", tags = "Meeting - Agent")
    public ResponseEntity<ApiResponse<MeetingResponse>> rejectMeeting(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long meetingId
    ) {
        MeetingResponse response =
                meetingService.rejectMeeting(
                        extractUserId(jwt),
                        meetingId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회의 예약 요청을 거절했습니다.",
                        response
                )
        );
    }

    @PatchMapping("/meetings/{meetingId}/preferred-times")
    @Operation(summary = "희망 회의 시간 수정", tags = "Meeting - Tenant")
    public ResponseEntity<ApiResponse<MeetingResponse>> updatePreferredTimes(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long meetingId,
            @Valid @RequestBody MeetingPreferredTimesUpdateRequest request
    ) {
        MeetingResponse response =
                meetingService.updatePreferredTimes(
                        extractUserId(jwt),
                        meetingId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "희망 회의 시간이 수정되었습니다.",
                        response
                )
        );
    }

    @DeleteMapping("/meetings/{meetingId}/cancel")
    @Operation(summary = "회의 취소", tags = "Meeting - Common")
    public ResponseEntity<ApiResponse<Void>> cancelMeeting(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long meetingId
    ) {
        meetingService.cancelMeeting(
                extractUserId(jwt),
                meetingId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회의가 취소되었습니다."
                )
        );
    }

    @GetMapping("/meetings")
    @Operation(summary = "내 회의 목록 조회", tags = "Meeting - Common")
    public ResponseEntity<ApiResponse<Page<MeetingResponse>>> getMeetings(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(defaultValue = "true") boolean includeCompleted,
            @RequestParam(defaultValue = "false") boolean includeCanceled
    ) {
        Page<MeetingResponse> response =
                meetingService.getMeetings(
                        extractUserId(jwt),
                        includeCompleted,
                        includeCanceled,
                        PageRequest.of(page, size, direction, "createdAt")
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회의 목록 조회가 완료되었습니다.",
                        response
                )
        );
    }

    @GetMapping("/meetings/{meetingId}")
    @Operation(summary = "회의 상세 조회", tags = "Meeting - Common")
    public ResponseEntity<ApiResponse<MeetingResponse>> getMeetingDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long meetingId
    ) {
        MeetingResponse response =
                meetingService.getMeetingDetail(
                        extractUserId(jwt),
                        meetingId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "회의 상세 조회가 완료되었습니다.",
                        response
                )
        );
    }

    @DeleteMapping("/meetings/{meetingId}")
    @Operation(summary = "완료된 회의 삭제", tags = "Meeting - Common")
    public ResponseEntity<ApiResponse<Void>> deleteCompletedMeeting(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long meetingId
    ) {
        meetingService.deleteCompletedMeeting(
                extractUserId(jwt),
                meetingId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "완료된 회의가 삭제되었습니다."
                )
        );
    }

    private Long extractUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
