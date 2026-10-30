package com.backend.bangbangbwa.domain.detection.dto;

import java.util.List;

public record AiCaptureSaveResult(
        boolean created,
        List<SessionCaptureDefectResponse> captures
) {

    public static AiCaptureSaveResult created(
            List<SessionCaptureDefectResponse> captures
    ) {
        return new AiCaptureSaveResult(true, captures);
    }

    public static AiCaptureSaveResult alreadyProcessed() {
        return new AiCaptureSaveResult(false, List.of());
    }
}
