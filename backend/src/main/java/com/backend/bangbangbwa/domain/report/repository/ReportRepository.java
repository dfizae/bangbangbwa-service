package com.backend.bangbangbwa.domain.report.repository;

import com.backend.bangbangbwa.domain.report.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @EntityGraph(attributePaths = {
            "meeting",
            "meeting.tenant",
            "meeting.agent"
    })
    Optional<Report> findByMeeting_Id(Long meetingId);

    /**
     * 동일 미팅의 동시 생성 요청을 PostgreSQL에서 원자적으로 처리합니다.
     * 이미 존재하면 아무 일도 하지 않고, 새로 생성된 경우에만 1을 반환합니다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO report (meeting_id, status, created_at)
            VALUES (:meetingId, 'DETECTED', clock_timestamp())
            ON CONFLICT (meeting_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("meetingId") Long meetingId);

    @EntityGraph(attributePaths = {
            "meeting",
            "meeting.tenant",
            "meeting.agent"
    })
    @Query("""
            SELECT r
            FROM Report r
            WHERE r.reportId = :reportId
            """)
    Optional<Report> findWithMeetingById(@Param("reportId") Long reportId);

    @EntityGraph(attributePaths = {
            "meeting",
            "meeting.tenant",
            "meeting.agent"
    })
    @Query("""
            SELECT r
            FROM Report r
            WHERE r.meeting.deletedAt IS NULL
              AND (
                    r.meeting.tenant.id = :userId
                    OR r.meeting.agent.id = :userId
                  )
            ORDER BY r.createdAt DESC
            """)
    Page<Report> findAllByParticipantId(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
