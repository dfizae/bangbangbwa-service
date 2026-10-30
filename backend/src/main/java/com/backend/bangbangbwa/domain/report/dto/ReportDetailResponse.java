package com.backend.bangbangbwa.domain.report.dto;

import com.backend.bangbangbwa.domain.checklist.dto.ChecklistResponse;
import com.backend.bangbangbwa.domain.report.entity.Report;
import com.backend.bangbangbwa.domain.report.entity.ReportStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 리포트 상세(GET /api/reports/{reportId}).
 * 매물 아이디, 체크리스트, 캡쳐 이미지 및 하자 분석 결과를 포함합니다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReportDetailResponse {

    private Long reportId;
    private ReportStatus status;
    private Long propertyId;
    private String summary;
    private LocalDateTime createdAt;
    private ChecklistResponse checklist;
    private List<CaptureResponse> captures;

    public static ReportDetailResponse of(
            Report report,
            ChecklistResponse checklist,
            List<CaptureResponse> captures
    ) {
        return new ReportDetailResponse(
                report.getReportId(),
                report.getStatus(),
                report.getMeeting().getPropertyId(),
                report.getSummary(),
                report.getCreatedAt(),
                checklist,
                captures
        );
    }
}
