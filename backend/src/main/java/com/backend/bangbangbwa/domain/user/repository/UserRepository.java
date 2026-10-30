package com.backend.bangbangbwa.domain.user.repository;

import com.backend.bangbangbwa.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndDeletedAtIsNull(Long userId);

    boolean existsByNicknameAndDeletedAtIsNullAndIdNot(String nickname,Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM User u
        WHERE u.id = :agentId
          AND u.deletedAt IS NULL
        """)
    Optional<User> findAgentForScheduleUpdate(
            @Param("agentId") Long agentId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM User u
        WHERE u.id = :userId
          AND u.deletedAt IS NULL
        """)
    Optional<User> findUserForScheduleUpdate(
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT agent
        FROM Meeting meeting
        JOIN meeting.agent agent
        WHERE meeting.id = :meetingId
          AND meeting.tenant.id = :tenantId
          AND meeting.deletedAt IS NULL
          AND agent.deletedAt IS NULL
        """)
    Optional<User> findAgentForMeetingScheduleUpdate(
            @Param("meetingId") Long meetingId,
            @Param("tenantId") Long tenantId
    );
}