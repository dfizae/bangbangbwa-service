package com.backend.bangbangbwa.controller;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.common.response.ApiResponse;
import com.backend.bangbangbwa.domain.report.dto.ReportDetailResponse;
import com.backend.bangbangbwa.domain.report.dto.ReportStatusResponse;
import com.backend.bangbangbwa.domain.report.dto.ReportSummaryResponse;
import com.backend.bangbangbwa.domain.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Report", description = "리포트 관련 API")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/sessions/{sessionId}/reports")
    @Operation(
            summary = "리포트 생성",
            description = "세션 종료 후 하자·체크리스트·캡쳐 이미지를 담은 리포트를 생성합니다."
    )
    public ResponseEntity<ApiResponse<ReportStatusResponse>> createReport(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long sessionId
    ) {
        ReportStatusResponse response =
                reportService.createReport(sessionId, extractUserId(jwt));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        "리포트 생성이 요청되었습니다.",
                        response
                ));
    }

    @GetMapping("/reports/{reportId}/status")
    @Operation(
            summary = "리포트 생성 상태 조회",
            description = "리포트 생성 진행 상태(DETECTED/CONFIRMED/REJECTED)를 조회합니다."
    )
    public ResponseEntity<ApiResponse<ReportStatusResponse>> getReportStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reportId
    ) {
        ReportStatusResponse response =
                reportService.getReportStatus(reportId, extractUserId(jwt));

        return ResponseEntity.ok(
                ApiResponse.success(
                        "리포트 상태 조회가 완료되었습니다.",
                        response
                )
        );
    }

    @GetMapping("/reports/me")
    @Operation(
            summary = "리포트 목록 조회",
            description = "로그인한 사용자가 참여한 미팅의 리포트 목록을 조회합니다."
    )
    public ResponseEntity<ApiResponse<Page<ReportSummaryResponse>>> getMyReports(
            @AuthenticationPrincipal Jwt jwt,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ReportSummaryResponse> response =
                reportService.getMyReports(extractUserId(jwt), pageable);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "리포트 목록 조회가 완료되었습니다.",
                        response
                )
        );
    }

    @GetMapping("/reports/{reportId}")
    @Operation(
            summary = "리포트 상세 조회",
            description = "매물 아이디, 체크리스트, 캡쳐 이미지 및 하자 분석 결과를 조회합니다."
    )
    public ResponseEntity<ApiResponse<ReportDetailResponse>> getReportDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reportId
    ) {
        ReportDetailResponse response =
                reportService.getReportDetail(reportId, extractUserId(jwt));

        return ResponseEntity.ok(
                ApiResponse.success(
                        "리포트 상세 조회가 완료되었습니다.",
                        response
                )
        );
    }

    @GetMapping("/reports/{reportId}/download")
    @Operation(
            summary = "리포트 PDF 다운로드",
            description = "완료된 리포트를 PDF 파일로 다운로드합니다."
    )
    public ResponseEntity<byte[]> downloadReport(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reportId
    ) {
        ReportService.ReportPdfContent pdf =
                reportService.downloadReport(reportId, extractUserId(jwt));

        ContentDisposition contentDisposition = ContentDisposition
                .attachment()
                .filename(pdf.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.content());
    }

    @DeleteMapping("/reports/{reportId}/delete")
    @Operation(
            summary = "리포트 삭제",
            description = "미팅의 임차인만 리포트를 삭제할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<Void>> deleteReport(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reportId
    ) {
        reportService.deleteReport(reportId, extractUserId(jwt));

        return ResponseEntity.ok()
                .body(ApiResponse.success("리포트가 삭제되었습니다."));
    }

    private Long extractUserId(Jwt jwt) {
        if (jwt == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }
}
