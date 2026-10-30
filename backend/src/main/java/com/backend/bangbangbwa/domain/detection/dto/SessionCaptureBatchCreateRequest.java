package com.backend.bangbangbwa.domain.detection.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class SessionCaptureBatchCreateRequest {

    @NotNull(message = "캡처 정보 목록은 필수입니다.")
    private List<@NotNull @Valid SessionCaptureCreateRequest> captures =
            new ArrayList<>();
}
