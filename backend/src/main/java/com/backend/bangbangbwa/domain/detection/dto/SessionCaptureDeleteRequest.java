package com.backend.bangbangbwa.domain.detection.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class SessionCaptureDeleteRequest {

    @NotEmpty(message = "삭제할 캡처 ID 목록은 필수입니다.")
    private List<@NotNull Long> captureIds = new ArrayList<>();
}
