package com.backend.bangbangbwa.domain.checklist.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name="checklist_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_id", nullable = false)
    private Checklist checklist;

    @Column(nullable = false, length = 255)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChecklistItemStatus status;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Builder
    private ChecklistItem(
            Checklist checklist,
            String content,
            String memo
    ) {
        this.checklist = checklist;
        this.content = content;
        this.memo = memo;
        this.status = ChecklistItemStatus.PENDING;
    }

    public void update(
            String content,
            String memo
    ) {
        this.content = content;
        this.memo = memo;
    }

    public void updateStatus(ChecklistItemStatus status) {
        this.status = status;
    }

}
