package com.backend.bangbangbwa.domain.detection.entity;

import com.backend.bangbangbwa.domain.session.entity.RtcSession;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Entity
@Table(
        name = "session_capture",
        indexes = {
                @Index(
                        name = "idx_session_capture_session_id",
                        columnList = "session_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionCapture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "capture_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_session_capture_session")
    )
    private RtcSession session;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @OneToMany(
            mappedBy = "capture",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Defect> defects = new ArrayList<>();

    @Builder
    private SessionCapture(
            RtcSession session,
            String imageUrl,
            LocalDateTime capturedAt
    ) {
        this.session = session;
        this.imageUrl = imageUrl;
        this.capturedAt = capturedAt;
    }

    @PrePersist
    private void prePersist() {
        if (this.capturedAt == null) {
            this.capturedAt = LocalDateTime.now();
        }
    }

    public List<Defect> getDefects() {
        return Collections.unmodifiableList(defects);
    }

    public void addDefect(Defect defect) {
        defects.add(defect);
        defect.assignCapture(this);
    }
}
