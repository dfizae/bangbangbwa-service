package com.backend.bangbangbwa.domain.report.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 캡쳐 이미지 1건과 해당 캡쳐에 대한 하자(defect) 분석 결과.
 * 실제 데이터는 detection 모듈({@code ReportCaptureProvider} 구현체)에서 제공됩니다.
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CaptureResponse {

    private Long captureId;
    private String imageUrl;
    private LocalDateTime capturedAt;
    private List<DefectResponse> defects;
}
