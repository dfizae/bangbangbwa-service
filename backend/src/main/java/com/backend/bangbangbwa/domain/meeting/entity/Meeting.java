package com.backend.bangbangbwa.domain.meeting.entity;

import com.backend.bangbangbwa.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "meeting")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_id")
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @Column(name = "preferred_at_1", nullable = false)
    private LocalDateTime preferredAt1;

    @Column(name = "preferred_at_2")
    private LocalDateTime preferredAt2;

    @Column(name = "preferred_at_3")
    private LocalDateTime preferredAt3;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeetingStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Meeting(
            Long propertyId,
            User tenant,
            User agent,
            LocalDateTime preferredAt1,
            LocalDateTime preferredAt2,
            LocalDateTime preferredAt3
    ) {
        this.propertyId = propertyId;
        this.tenant = tenant;
        this.agent = agent;
        this.preferredAt1 = preferredAt1;
        this.preferredAt2 = preferredAt2;
        this.preferredAt3 = preferredAt3;
        this.status = MeetingStatus.REQUESTED;
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void confirm(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
        this.status = MeetingStatus.CONFIRMED;
    }

    public void reject() {
        this.status = MeetingStatus.REJECTED;
    }

    public void updatePreferredTimes(
            LocalDateTime preferredAt1,
            LocalDateTime preferredAt2,
            LocalDateTime preferredAt3
    ) {
        this.preferredAt1 = preferredAt1;
        this.preferredAt2 = preferredAt2;
        this.preferredAt3 = preferredAt3;
    }

    public void cancel() {
        this.status = MeetingStatus.CANCELED;
    }

    /**
     * RTC 세션이 종료되면 회의를 완료 상태로 전환한다.
     * 확정된 회의에서만 완료로 넘어가며, 이미 완료된 경우 중복 호출을 무시한다.
     */
    public void complete() {
        if (this.status == MeetingStatus.CONFIRMED) {
            this.status = MeetingStatus.COMPLETED;
        }
    }

    /**
     * 종료 후 보관 기간이 지난 회의를 목록에서 숨기기 위한 소프트 삭제.
     * deletedAt 이 설정되면 모든 조회 쿼리(deletedAt IS NULL)에서 제외된다.
     */
    public void softDelete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isParticipant(Long userId) {
        return (tenant != null && tenant.getId().equals(userId))
                || agent.getId().equals(userId);
    }

    public boolean isTenant(Long userId) {
        return tenant != null && tenant.getId().equals(userId);
    }

    public boolean isAgent(Long userId) {
        return agent.getId().equals(userId);
    }
}
