package com.backend.bangbangbwa.domain.checklist.repository;

import com.backend.bangbangbwa.domain.checklist.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChecklistItemRepository
        extends JpaRepository<ChecklistItem, Long> {

    List<ChecklistItem> findAllByChecklist_IdOrderByIdAsc(
            Long checklistId
    );

    long countByChecklist_Id(Long checklistId);

    Optional<ChecklistItem> findByIdAndChecklist_Id(
            Long itemId,
            Long checklistId
    );

    void deleteAllByChecklist_Id(Long checklistId);
}