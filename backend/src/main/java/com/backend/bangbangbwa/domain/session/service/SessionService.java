package com.backend.bangbangbwa.domain.session.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.meeting.entity.Meeting;
import com.backend.bangbangbwa.domain.meeting.entity.MeetingStatus;
import com.backend.bangbangbwa.domain.meeting.repository.MeetingRepository;
import com.backend.bangbangbwa.domain.session.dto.SessionCreateResponse;
import com.backend.bangbangbwa.domain.session.dto.SessionEndResponse;
import com.backend.bangbangbwa.domain.session.dto.SessionJoinResponse;
import com.backend.bangbangbwa.domain.session.dto.SessionLeaveResponse;
import com.backend.bangbangbwa.domain.session.entity.RtcSession;
import com.backend.bangbangbwa.domain.session.event.RtcSessionClosedEvent;
import com.backend.bangbangbwa.domain.session.repository.SessionRepository;
import com.backend.bangbangbwa.domain.session.signaling.SignalHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private static final int CONCURRENCY_RETRY_COUNT = 2;

    private final SessionRepository sessionRepository;
    private final MeetingRepository meetingRepository;
    private final SessionJoinExecutor sessionJoinExecutor;
    private final SessionExpirationService sessionExpirationService;
    private final SignalHandler signalHandler;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.rtc.join-base-url}")
    private String joinBaseUrl;

    @Transactional
    public SessionCreateResponse createSession(
            Long meetingId,
            Long currentUserId
    ) {
        Meeting meeting = getMeeting(meetingId);

        validateMeetingParticipant(
                meeting,
                currentUserId
        );
        validateConfirmedMeeting(meeting);

        RtcSession existingSession =
                sessionRepository.findByMeeting_Id(meetingId)
                        .orElse(null);

        if (existingSession != null) {
            return createExistingSessionResponse(
                    existingSession
            );
        }

        sessionRepository.insertIfAbsent(
                meetingId,
                UUID.randomUUID().toString()
        );

        /*
         * 동시 요청 중 한 요청이 먼저 생성했더라도,
         * INSERT 후 실제 DB에 존재하는 동일 세션을 다시 조회합니다.
         */
        RtcSession session =
                sessionRepository.findByMeeting_Id(meetingId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.SESSION_NOT_FOUND
                                )
                        );

        return createExistingSessionResponse(session);
    }

    public SessionJoinResponse joinSession(
            Long sessionId,
            Long currentUserId
    ) {
        SessionConcurrencyException lastException = null;

        for (
                int attempt = 0;
                attempt < CONCURRENCY_RETRY_COUNT;
                attempt++
        ) {
            try {
                return sessionJoinExecutor.joinOnce(
                        sessionId,
                        currentUserId
                );
            } catch (
                    SessionConcurrencyException exception
            ) {
                lastException = exception;
            }
        }

        throw lastException;
    }

    @Transactional
    public SessionLeaveResponse leaveSession(
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

        signalHandler.closeConnection(
                session.getRoomToken(),
                currentUserId
        );

        return SessionLeaveResponse.builder()
                .sessionId(session.getSessionId())
                .status(session.getStatus())
                .leftAt(now)
                .build();
    }

    @Transactional
    public SessionEndResponse endSession(
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

        if (session.isEnded()) {
            throw new CustomException(
                    ErrorCode.SESSION_ALREADY_ENDED
            );
        }

        LocalDateTime endedAt = LocalDateTime.now();

        int updatedCount =
                sessionRepository.endIfUnchanged(
                        session.getSessionId(),
                        session.getUpdatedAt(),
                        endedAt
                );

        if (updatedCount == 0) {
            throw new SessionConcurrencyException(
                    "다른 요청이 RTC 세션 상태를 먼저 변경했습니다."
            );
        }

        RtcSession endedSession =
                getSession(sessionId);

        /*
         * 세션이 종료되면 연결된 회의를 완료 상태로 전환합니다.
         * 완료 시점(session.ended_at)을 기준으로 보관/정리가 이뤄집니다.
         */
        endedSession.getMeeting().complete();

        /*
         * 이벤트는 현재 트랜잭션 커밋 이후 처리됩니다.
         * DB 커밋 실패 시 WebSocket 방은 닫히지 않습니다.
         */
        eventPublisher.publishEvent(
                new RtcSessionClosedEvent(
                        endedSession.getRoomToken()
                )
        );

        return SessionEndResponse.from(
                endedSession
        );
    }

    private SessionCreateResponse createExistingSessionResponse(
            RtcSession session
    ) {
        if (session.isEnded()) {
            throw new CustomException(
                    ErrorCode.SESSION_ALREADY_ENDED
            );
        }

        if (session.isExpired(LocalDateTime.now())) {
            sessionExpirationService.expire(
                    session.getSessionId(),
                    session.getUpdatedAt()
            );

            throw new CustomException(
                    ErrorCode.SESSION_EXPIRED
            );
        }

        return SessionCreateResponse.from(
                session,
                joinBaseUrl
                        + "/"
                        + session.getSessionId()
        );
    }

    private Meeting getMeeting(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.MEETING_NOT_FOUND
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
}
