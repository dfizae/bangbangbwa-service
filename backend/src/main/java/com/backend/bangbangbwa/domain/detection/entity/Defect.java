package com.backend.bangbangbwa.domain.detection.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "defect",
        indexes = {
                @Index(
                        name = "idx_defect_capture_id",
                        columnList = "capture_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Defect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "defect_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "capture_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_defect_capture")
    )
    private SessionCapture capture;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private DefectType type;

    @Column(name = "confidence", nullable = false)
    private Double confidence;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "bbox_x_center")
    private Double bboxXCenter;

    @Column(name = "bbox_y_center")
    private Double bboxYCenter;

    @Column(name = "bbox_width")
    private Double bboxWidth;

    @Column(name = "bbox_height")
    private Double bboxHeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DefectStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Defect(
            DefectType type,
            Double confidence,
            String description,
            Double bboxXCenter,
            Double bboxYCenter,
            Double bboxWidth,
            Double bboxHeight,
            DefectStatus status
    ) {
        this.type = type;
        this.confidence = confidence;
        this.description = description;
        this.bboxXCenter = bboxXCenter;
        this.bboxYCenter = bboxYCenter;
        this.bboxWidth = bboxWidth;
        this.bboxHeight = bboxHeight;
        this.status = status != null ? status : DefectStatus.DETECTED;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = DefectStatus.DETECTED;
        }
    }

    void assignCapture(SessionCapture capture) {
        this.capture = capture;
    }
}
