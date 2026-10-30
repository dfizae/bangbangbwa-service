package com.backend.bangbangbwa.domain.report.dto;

import com.backend.bangbangbwa.domain.report.entity.Report;
import com.backend.bangbangbwa.domain.report.entity.ReportStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 리포트 목록(GET /api/reports/me) 항목.
 * 매물 제목, 매물 대표 이미지, 세션(미팅) 날짜를 포함합니다.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReportSummaryResponse {

    private Long reportId;
    private ReportStatus status;
    private Long propertyId;
    private String propertyTitle;
    private String thumbnailUrl;
    private LocalDateTime meetingDate;
    private LocalDateTime createdAt;

    public static ReportSummaryResponse of(
            Report report,
            String propertyTitle,
            String thumbnailUrl
    ) {
        return new ReportSummaryResponse(
                report.getReportId(),
                report.getStatus(),
                report.getMeeting().getPropertyId(),
                propertyTitle,
                thumbnailUrl,
                report.getMeeting().getScheduledAt(),
                report.getCreatedAt()
        );
    }
}
