package com.backend.bangbangbwa.domain.session.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.meeting.entity.Meeting;
import com.backend.bangbangbwa.domain.meeting.entity.MeetingStatus;
import com.backend.bangbangbwa.domain.session.dto.SessionJoinResponse;
import com.backend.bangbangbwa.domain.session.entity.RtcSession;
import com.backend.bangbangbwa.domain.session.repository.SessionRepository;
import com.backend.bangbangbwa.domain.session.signaling.SignalingTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SessionJoinExecutor {

    private static final long SESSION_DURATION_MINUTES = 30L;

    private final SessionRepository sessionRepository;
    private final SignalingTokenService signalingTokenService;
    private final SessionExpirationService sessionExpirationService;

    @Value("${app.rtc.signaling-url}")
    private String signalingUrl;

    /**
     * 재시도할 때마다 새 트랜잭션과 새 영속성 컨텍스트에서 조회합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SessionJoinResponse joinOnce(
            Long sessionId,
            Long currentUserId
    ) {
        RtcSession session = getSession(sessionId);
        Meeting meeting = session.getMeeting();

        validateMeetingParticipant(
                meeting,
                currentUserId
        );
        validateConfirmedMeeting(meeting);

        LocalDateTime now = LocalDateTime.now();

        if (session.isEnded()) {
            throw new CustomException(
                    ErrorCode.SESSION_ALREADY_ENDED
            );
        }

        if (session.isExpired(now)) {
            sessionExpirationService.expire(
                    session.getSessionId(),
                    session.getUpdatedAt()
            );

            throw new CustomException(
                    ErrorCode.SESSION_EXPIRED
            );
        }

        /*
         * CREATED 상태에서만 이 값이 실제 저장됩니다.
         * ACTIVE 재접속에서는 Repository의 COALESCE로 기존 만료 시각을 유지합니다.
         */
        LocalDateTime expiresAt =
                now.plusMinutes(SESSION_DURATION_MINUTES);

        int updatedCount =
                sessionRepository.activateIfUnchanged(
                        session.getSessionId(),
                        session.getUpdatedAt(),
                        now,
                        expiresAt
                );

        if (updatedCount == 0) {
            throw new SessionConcurrencyException(
                    "다른 요청이 RTC 세션 상태를 먼저 변경했습니다."
            );
        }

        RtcSession updatedSession =
                getSession(sessionId);

        String signalingToken =
                signalingTokenService.createToken(
                        currentUserId,
                        updatedSession.getRoomToken()
                );

        return SessionJoinResponse.from(
                updatedSession,
                createSignalingUrl(
                        updatedSession,
                        signalingToken
                )
        );
    }

    private RtcSession getSession(Long sessionId) {
        return sessionRepository
                .findWithMeetingById(sessionId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.SESSION_NOT_FOUND
                        )
                );
    }

    private void validateMeetingParticipant(
            Meeting meeting,
            Long currentUserId
    ) {
        if (!meeting.isParticipant(currentUserId)) {
            throw new CustomException(
                    ErrorCode.SESSION_ACCESS_DENIED
            );
        }
    }

    private void validateConfirmedMeeting(Meeting meeting) {
        if (meeting.getStatus() != MeetingStatus.CONFIRMED) {
            throw new CustomException(
                    ErrorCode.INVALID_MEETING_STATUS
            );
        }
    }

    private String createSignalingUrl(
            RtcSession session,
            String signalingToken
    ) {
        return UriComponentsBuilder
                .fromUriString(signalingUrl)
                .queryParam(
                        "roomToken",
                        session.getRoomToken()
                )
                .queryParam(
                        "accessToken",
                        signalingToken
                )
                .build()
                .encode()
                .toUriString();
    }
}
