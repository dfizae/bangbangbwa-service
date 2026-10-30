package com.backend.bangbangbwa.domain.session.entity;

import com.backend.bangbangbwa.domain.meeting.entity.Meeting;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "\"session\"",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_session_meeting",
                        columnNames = "meeting_id"
                ),
                @UniqueConstraint(
                        name = "uq_session_room_token",
                        columnNames = "room_token"
                )
        },
        indexes = {
                @Index(
                        name = "idx_session_status_expires_at",
                        columnList = "status, expires_at"
                )
        }
)
public class RtcSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "meeting_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_session_meeting")
    )
    private Meeting meeting;

    @Column(name = "room_token", nullable = false, length = 255)
    private String roomToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private RtcSession(Meeting meeting, String roomToken) {
        this.meeting = meeting;
        this.roomToken = roomToken;
        this.status = SessionStatus.CREATED;
    }

    public static RtcSession create(
            Meeting meeting,
            String roomToken
    ) {
        return new RtcSession(meeting, roomToken);
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean isExpired(LocalDateTime now) {
        return this.expiresAt != null
                && !now.isBefore(this.expiresAt);
    }

    public boolean isEnded() {
        return this.status == SessionStatus.ENDED;
    }
}
