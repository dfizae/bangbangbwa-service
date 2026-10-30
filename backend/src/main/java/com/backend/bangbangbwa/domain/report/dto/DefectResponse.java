package com.backend.bangbangbwa.domain.report.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DefectResponse {

    private Long defectId;
    private String type;
    private Double confidence;
    private String description;
    private Double bboxXCenter;
    private Double bboxYCenter;
    private Double bboxWidth;
    private Double bboxHeight;
    private String status;
}
