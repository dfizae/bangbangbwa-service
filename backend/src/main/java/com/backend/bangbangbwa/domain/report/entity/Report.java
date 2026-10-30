package com.backend.bangbangbwa.domain.report.entity;

import com.backend.bangbangbwa.domain.meeting.entity.Meeting;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "report",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_report_meeting",
                        columnNames = "meeting_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "meeting_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_report_meeting")
    )
    private Meeting meeting;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ReportStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Report(Meeting meeting) {
        this.meeting = meeting;
        this.status = ReportStatus.DETECTED;
    }

    public static Report create(Meeting meeting) {
        return new Report(meeting);
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void applySummary(String summary) {
        this.summary = summary;
    }

    public void markConfirmed() {
        this.status = ReportStatus.CONFIRMED;
    }

    public void markRejected() {
        this.status = ReportStatus.REJECTED;
    }

    public boolean isConfirmed() {
        return this.status == ReportStatus.CONFIRMED;
    }
}
