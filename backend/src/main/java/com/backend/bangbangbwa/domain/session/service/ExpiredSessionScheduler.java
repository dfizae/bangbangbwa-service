package com.backend.bangbangbwa.domain.session.service;

import com.backend.bangbangbwa.domain.session.entity.RtcSession;
import com.backend.bangbangbwa.domain.session.entity.SessionStatus;
import com.backend.bangbangbwa.domain.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExpiredSessionScheduler {

    private final SessionRepository sessionRepository;
    private final SessionExpirationService sessionExpirationService;

    /**
     * 최대 1분 오차로 만료된 세션을 자동 종료합니다.
     * 각 세션은 REQUIRES_NEW 트랜잭션에서 독립적으로 처리됩니다.
     */
    @Scheduled(
            fixedDelayString =
                    "${app.rtc.expiration-check-delay-ms:60000}"
    )
    public void closeExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();

        List<RtcSession> sessions =
                sessionRepository.findExpiredSessions(
                        SessionStatus.ACTIVE,
                        now
                );

        for (RtcSession session : sessions) {
            try {
                sessionExpirationService.expire(
                        session.getSessionId(),
                        session.getUpdatedAt()
                );
            } catch (SessionConcurrencyException ignored) {
                /*
                 * 조회 이후 다른 요청이 세션을 변경한 경우입니다.
                 * 다음 스케줄에서 최신 상태를 다시 확인합니다.
                 */
            }
        }
    }
}
