package com.backend.bangbangbwa.domain.detection.dto;

import com.backend.bangbangbwa.domain.detection.entity.Defect;
import com.backend.bangbangbwa.domain.detection.entity.DefectStatus;
import com.backend.bangbangbwa.domain.detection.entity.DefectType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DefectResponse {

    private Long defectId;
    private DefectType type;
    private Double confidence;
    private String description;
    private Double bboxXCenter;
    private Double bboxYCenter;
    private Double bboxWidth;
    private Double bboxHeight;
    private DefectStatus status;
    private LocalDateTime createdAt;

    public static DefectResponse from(Defect defect) {
        return new DefectResponse(
                defect.getId(),
                defect.getType(),
                defect.getConfidence(),
                defect.getDescription(),
                defect.getBboxXCenter(),
                defect.getBboxYCenter(),
                defect.getBboxWidth(),
                defect.getBboxHeight(),
                defect.getStatus(),
                defect.getCreatedAt()
        );
    }
}
