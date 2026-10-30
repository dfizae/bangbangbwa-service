package com.backend.bangbangbwa.domain.property.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PropertyStatusUpdateRequest {

    @NotBlank(message = "매물 상태는 필수입니다.")
    private String status;
}
