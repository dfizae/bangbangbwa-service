package com.backend.bangbangbwa.domain.checklist.repository;

import com.backend.bangbangbwa.domain.checklist.entity.Checklist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChecklistRepository extends JpaRepository<Checklist, Long> {

    @EntityGraph(attributePaths = {
            "meeting",
            "meeting.tenant",
            "meeting.agent"
    })
    Optional<Checklist> findByMeeting_Id(Long meetingId);

    boolean existsByMeeting_Id(Long meetingId);
}