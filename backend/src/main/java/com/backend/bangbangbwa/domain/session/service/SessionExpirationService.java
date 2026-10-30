package com.backend.bangbangbwa.domain.session.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.session.entity.RtcSession;
import com.backend.bangbangbwa.domain.session.event.RtcSessionClosedEvent;
import com.backend.bangbangbwa.domain.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SessionExpirationService {

    private final SessionRepository sessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 호출한 쪽에서 이후 예외를 던지더라도 만료 처리가 롤백되지 않도록
     * 독립된 트랜잭션에서 종료합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expire(
            Long sessionId,
            LocalDateTime expectedUpdatedAt
    ) {
        LocalDateTime now = LocalDateTime.now();

        int updatedCount = sessionRepository.endIfUnchanged(
                sessionId,
                expectedUpdatedAt,
                now
        );

        if (updatedCount == 0) {
            RtcSession latest = sessionRepository
                    .findWithMeetingById(sessionId)
                    .orElseThrow(() ->
                            new CustomException(
                                    ErrorCode.SESSION_NOT_FOUND
                            )
                    );

            if (!latest.isEnded()) {
                throw new SessionConcurrencyException(
                        "만료 처리 중 RTC 세션 상태가 변경되었습니다."
                );
            }

            return;
        }

        RtcSession endedSession = sessionRepository
                .findWithMeetingById(sessionId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.SESSION_NOT_FOUND
                        )
                );

        /*
         * 만료로 종료된 세션의 회의도 완료 상태로 전환합니다.
         */
        endedSession.getMeeting().complete();

        eventPublisher.publishEvent(
                new RtcSessionClosedEvent(
                        endedSession.getRoomToken()
                )
        );
    }
}
