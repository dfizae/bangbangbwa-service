package com.backend.bangbangbwa.domain.meeting.repository;

import com.backend.bangbangbwa.domain.meeting.entity.Meeting;
import com.backend.bangbangbwa.domain.meeting.entity.MeetingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m
            FROM Meeting m
            WHERE m.id = :meetingId
              AND m.deletedAt IS NULL
              AND (
                    m.tenant.id = :userId
                    OR m.agent.id = :userId
                  )
            """)
    Optional<Meeting> findAccessibleMeeting(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    boolean existsByPropertyIdAndTenant_IdAndStatusInAndDeletedAtIsNull(
            Long propertyId,
            Long tenantId,
            Collection<MeetingStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m
            FROM Meeting m
            WHERE m.id = :meetingId
              AND m.deletedAt IS NULL
              AND m.agent.id = :agentId
            """)
    Optional<Meeting> findByIdAndAgentId(
            @Param("meetingId") Long meetingId,
            @Param("agentId") Long agentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m
            FROM Meeting m
            WHERE m.id = :meetingId
              AND m.deletedAt IS NULL
              AND m.tenant.id = :tenantId
            """)
    Optional<Meeting> findByIdAndTenantId(
            @Param("meetingId") Long meetingId,
            @Param("tenantId") Long tenantId
    );

    @Query("""
            SELECT DISTINCT m.scheduledAt
            FROM Meeting m
            WHERE m.scheduledAt >= :from
              AND m.scheduledAt < :to
              AND m.status = :status
              AND m.deletedAt IS NULL
              AND m.agent.id = :agentId
            ORDER BY m.scheduledAt ASC
            """)
    List<LocalDateTime> findUnavailableTimesForAgentAndProperty(
            @Param("agentId") Long agentId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") MeetingStatus status
    );

    @EntityGraph(attributePaths = {
            "tenant",
            "agent"
    })
    @Query("""
            SELECT m
            FROM Meeting m
            WHERE m.id = :meetingId
              AND m.deletedAt IS NULL
              AND (
                    m.tenant.id = :userId
                    OR m.agent.id = :userId
                  )
            """)
    Optional<Meeting> findMeetingDetail(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );

    @EntityGraph(attributePaths = {
            "tenant",
            "agent"
    })
    @Query("""
            SELECT m
            FROM Meeting m
            WHERE m.propertyId = :propertyId
              AND m.status = :status
              AND m.deletedAt IS NULL
            """)
    Page<Meeting> findAllRequestedByPropertyId(
            @Param("propertyId") Long propertyId,
            @Param("status") MeetingStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "tenant",
            "agent"
    })
    @Query("""
            SELECT m
            FROM Meeting m
            WHERE m.propertyId = :propertyId
              AND m.agent.id = :agentId
              AND m.status = :status
              AND m.deletedAt IS NULL
            """)
    Page<Meeting> findAllByPropertyIdAndAgentId(
            @Param("propertyId") Long propertyId,
            @Param("agentId") Long agentId,
            @Param("status") MeetingStatus status,
            Pageable pageable
    );

    boolean existsByAgent_IdAndScheduledAtAndStatusAndDeletedAtIsNull(
            Long agentId,
            LocalDateTime scheduledAt,
            MeetingStatus status
    );

    boolean existsByTenant_IdAndScheduledAtAndStatusAndDeletedAtIsNull(
            Long tenantId,
            LocalDateTime scheduledAt,
            MeetingStatus status
    );

    @EntityGraph(attributePaths = {
            "tenant",
            "agent"
    })
    @Query("""
            SELECT m
            FROM Meeting m
            WHERE m.deletedAt IS NULL
              AND (
                    :includeCompleted = true
                    OR m.status <> :completedStatus
                  )
              AND (
                    :includeCanceled = true
                    OR m.status <> :canceledStatus
                  )
              AND (
                    m.tenant.id = :userId
                    OR m.agent.id = :userId
                  )
            """)
    Page<Meeting> findAllByParticipantId(
            @Param("userId") Long userId,
            @Param("includeCompleted") boolean includeCompleted,
            @Param("completedStatus") MeetingStatus completedStatus,
            @Param("includeCanceled") boolean includeCanceled,
            @Param("canceledStatus") MeetingStatus canceledStatus,
            Pageable pageable
    );

    boolean existsByPropertyIdAndScheduledAtInAndStatusAndDeletedAtIsNull(
            Long propertyId,
            Collection<LocalDateTime> scheduledTimes,
            MeetingStatus status
    );

    /**
     * 보관 기간이 지나 정리 대상이 되는 종료 회의의 ID를 조회한다.
     *   완료(COMPLETED): 연결된 세션의 ended_at 이 기준 시각보다 이전
     *   거절/취소(REJECTED, CANCELED): 상태 전환 시각(updatedAt)이 기준 시각보다 이전
     * 이미 소프트 삭제된(deletedAt IS NOT NULL) 회의는 제외한다.
     */
    @Query("""
            SELECT m.id
            FROM Meeting m
            LEFT JOIN RtcSession s ON s.meeting = m
            WHERE m.deletedAt IS NULL
              AND (
                    (
                          m.status = :completedStatus
                      AND s.endedAt IS NOT NULL
                      AND s.endedAt < :threshold
                    )
                 OR (
                          m.status IN :terminalStatuses
                      AND m.updatedAt < :threshold
                    )
              )
            """)
    List<Long> findCleanupTargetIds(
            @Param("completedStatus") MeetingStatus completedStatus,
            @Param("terminalStatuses") Collection<MeetingStatus> terminalStatuses,
            @Param("threshold") LocalDateTime threshold
    );
}
