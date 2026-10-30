package com.backend.bangbangbwa.domain.detection.dto;

import com.backend.bangbangbwa.domain.detection.entity.DefectType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DefectCreateRequest {

    @NotNull(message = "하자 유형은 필수입니다.")
    private DefectType type;

    @NotNull(message = "탐지 신뢰도는 필수입니다.")
    @DecimalMin(value = "0.0", message = "탐지 신뢰도는 0 이상이어야 합니다.")
    @DecimalMax(value = "1.0", message = "탐지 신뢰도는 1 이하여야 합니다.")
    private Double confidence;

    @Size(max = 1000, message = "하자 설명은 1000자 이하여야 합니다.")
    private String description;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double bboxXCenter;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double bboxYCenter;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double bboxWidth;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double bboxHeight;
}
