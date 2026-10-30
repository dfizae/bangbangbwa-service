package com.backend.bangbangbwa.domain.agent.repository;

import com.backend.bangbangbwa.domain.agent.entity.AgentVerificationRequest;
import com.backend.bangbangbwa.domain.agent.entity.AgentVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface AgentVerificationRepository
        extends JpaRepository<AgentVerificationRequest, Long> {

    Optional<AgentVerificationRequest> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select verification
            from AgentVerificationRequest verification
            where verification.userId = :userId
            """)
    Optional<AgentVerificationRequest> findByUserIdForUpdate(
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select verification
            from AgentVerificationRequest verification
            where verification.verificationId = :verificationId
            """)
    Optional<AgentVerificationRequest> findByIdForUpdate(
            @Param("verificationId") Long verificationId
    );

    Page<AgentVerificationRequest> findAllByStatus(
            AgentVerificationStatus status,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    update users
                    set role = 'AGENT',
                        updated_at = clock_timestamp()
                    where user_id = :userId
                    """,
            nativeQuery = true
    )
    int promoteUserToAgent(@Param("userId") Long userId);
}
