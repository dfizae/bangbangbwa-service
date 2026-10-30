package com.backend.bangbangbwa.domain.detection.repository;

import com.backend.bangbangbwa.domain.detection.entity.SessionCapture;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionCaptureRepository
        extends JpaRepository<SessionCapture, Long> {

    List<SessionCapture> findBySession_SessionIdOrderByCapturedAtDesc(
            Long sessionId
    );

    List<SessionCapture> findBySession_SessionId(Long sessionId);

    boolean existsByIdAndSession_SessionId(
            Long captureId,
            Long sessionId
    );

    /**
     * 미팅에 속한 세션의 캡쳐를 하자와 함께 캡쳐 시각 순으로 조회합니다.
     * 리포트 조립 시 하자(defect) N+1을 피하기 위해 {@link EntityGraph}로 함께 로딩합니다.
     */
    @EntityGraph(attributePaths = "defects")
    List<SessionCapture> findBySession_Meeting_IdOrderByCapturedAtAsc(
            Long meetingId
    );

    /**
     * 세션의 캡처를 하자와 함께 캡처 시각 내림차순으로 조회합니다.
     * 하자가 검출된 캡처만 추려낼 때 defect N+1을 피하기 위해
     * {@link EntityGraph}로 함께 로딩합니다.
     */
    @EntityGraph(attributePaths = "defects")
    List<SessionCapture>
    findWithDefectsBySession_SessionIdOrderByCapturedAtDesc(
            Long sessionId
    );

    List<SessionCapture> findByIdInAndSession_SessionId(
            List<Long> captureIds,
            Long sessionId
    );
}
