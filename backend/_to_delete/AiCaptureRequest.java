package com.backend.bangbangbwa.domain.detection.entity;

import com.backend.bangbangbwa.domain.session.entity.RtcSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_capture_request")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiCaptureRequest {

    @Id
    @Column(name = "idempotency_key", length = 200)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ai_capture_request_session")
    )
    private RtcSession session;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    private AiCaptureRequest(String idempotencyKey, RtcSession session) {
        this.idempotencyKey = idempotencyKey;
        this.session = session;
    }

    public static AiCaptureRequest completed(
            String idempotencyKey,
            RtcSession session
    ) {
        return new AiCaptureRequest(idempotencyKey, session);
    }

    @PrePersist
    private void prePersist() {
        this.processedAt = LocalDateTime.now();
    }
}
