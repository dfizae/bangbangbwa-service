package com.backend.bangbangbwa.domain.detection.dto;

import com.backend.bangbangbwa.domain.detection.entity.SessionCapture;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SessionCaptureResponse {

    private Long captureId;
    private String imageUrl;
    private LocalDateTime capturedAt;

    public static SessionCaptureResponse from(
            SessionCapture capture,
            String imageUrl
    ) {
        return new SessionCaptureResponse(
                capture.getId(),
                imageUrl,
                capture.getCapturedAt()
        );
    }
}
