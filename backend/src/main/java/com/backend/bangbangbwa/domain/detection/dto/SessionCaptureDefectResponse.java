package com.backend.bangbangbwa.domain.detection.dto;

import com.backend.bangbangbwa.domain.detection.entity.SessionCapture;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SessionCaptureDefectResponse {

    private Long captureId;
    private Long sessionId;
    private String imageUrl;
    private LocalDateTime capturedAt;
    private List<DefectResponse> defects;

    public static SessionCaptureDefectResponse from(
            SessionCapture capture,
            String imageUrl
    ) {
        List<DefectResponse> defects = capture.getDefects().stream()
                .map(DefectResponse::from)
                .toList();

        return new SessionCaptureDefectResponse(
                capture.getId(),
                capture.getSession().getSessionId(),
                imageUrl,
                capture.getCapturedAt(),
                defects
        );
    }
}
