package com.backend.bangbangbwa.domain.detection.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class SessionCaptureCreateRequest {

    private LocalDateTime capturedAt;

    @NotNull(message = "탐지 결과 목록은 필수입니다.")
    private List<@NotNull @Valid DefectCreateRequest> defects =
            new ArrayList<>();
}
