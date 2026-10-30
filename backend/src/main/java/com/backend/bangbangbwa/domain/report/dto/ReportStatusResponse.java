package com.backend.bangbangbwa.domain.report.dto;

import com.backend.bangbangbwa.domain.report.entity.Report;
import com.backend.bangbangbwa.domain.report.entity.ReportStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReportStatusResponse {

    private Long reportId;
    private ReportStatus status;

    public static ReportStatusResponse from(Report report) {
        return new ReportStatusResponse(
                report.getReportId(),
                report.getStatus()
        );
    }
}
