package com.backend.bangbangbwa.domain.session.repository;

import com.backend.bangbangbwa.domain.session.entity.RtcSession;
import com.backend.bangbangbwa.domain.session.entity.SessionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface SessionRepository
        extends JpaRepository<RtcSession, Long> {

    @EntityGraph(attributePaths = {
            "meeting",
            "meeting.tenant",
            "meeting.agent"
    })
    Optional<RtcSession> findByMeeting_Id(Long meetingId);

    @EntityGraph(attributePaths = {
            "meeting",
            "meeting.tenant",
            "meeting.agent"
    })
    Optional<RtcSession> findByRoomToken(String roomToken);

    @EntityGraph(attributePaths = {
            "meeting",
            "meeting.tenant",
            "meeting.agent"
    })
    @Query("""
            SELECT s
            FROM RtcSession s
            WHERE s.sessionId = :sessionId
            """)
    Optional<RtcSession> findWithMeetingById(
            @Param("sessionId") Long sessionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "meeting",
            "meeting.tenant",
            "meeting.agent"
    })
    @Query("""
            SELECT s
            FROM RtcSession s
            WHERE s.sessionId = :sessionId
            """)
    Optional<RtcSession> findWithMeetingByIdForUpdate(
            @Param("sessionId") Long sessionId
    );

    /**
     * 조회 당시 updated_at과 현재 DB의 updated_at이 같을 때만 활성화합니다.
     * 최초 입장일 때만 started_at, expires_at을 설정하고 재접속 시에는 유지합니다.
     */
    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query(value = """
            UPDATE "session"
               SET status = 'ACTIVE',
                   started_at = COALESCE(started_at, :startedAt),
                   expires_at = COALESCE(expires_at, :expiresAt),
                   updated_at = clock_timestamp()
             WHERE session_id = :sessionId
               AND updated_at = :expectedUpdatedAt
               AND status <> 'ENDED'
               AND (
                    expires_at IS NULL
                    OR expires_at > :startedAt
               )
            """, nativeQuery = true)
    int activateIfUnchanged(
            @Param("sessionId") Long sessionId,
            @Param("expectedUpdatedAt")
            LocalDateTime expectedUpdatedAt,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("expiresAt") LocalDateTime expiresAt
    );

    /**
     * 조회 당시 updated_at이 그대로인 경우에만 종료합니다.
     */
    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query(value = """
            UPDATE "session"
               SET status = 'ENDED',
                   ended_at = :endedAt,
                   updated_at = clock_timestamp()
             WHERE session_id = :sessionId
               AND updated_at = :expectedUpdatedAt
               AND status <> 'ENDED'
            """, nativeQuery = true)
    int endIfUnchanged(
            @Param("sessionId") Long sessionId,
            @Param("expectedUpdatedAt")
            LocalDateTime expectedUpdatedAt,
            @Param("endedAt") LocalDateTime endedAt
    );

    /**
     * 동일 미팅의 동시 생성 요청을 PostgreSQL에서 원자적으로 처리합니다.
     */
    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query(value = """
            INSERT INTO "session" (
                meeting_id,
                room_token,
                status,
                created_at,
                updated_at
            )
            VALUES (
                :meetingId,
                :roomToken,
                'CREATED',
                clock_timestamp(),
                clock_timestamp()
            )
            ON CONFLICT (meeting_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("meetingId") Long meetingId,
            @Param("roomToken") String roomToken
    );

    @EntityGraph(attributePaths = {
            "meeting",
            "meeting.tenant",
            "meeting.agent"
    })
    @Query("""
            SELECT s
            FROM RtcSession s
            WHERE s.status = :status
              AND s.expiresAt IS NOT NULL
              AND s.expiresAt <= :now
            """)
    List<RtcSession> findExpiredSessions(
            @Param("status") SessionStatus status,
            @Param("now") LocalDateTime now
    );
}
