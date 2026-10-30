package com.backend.bangbangbwa.domain.session.signaling;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.meeting.entity.Meeting;
import com.backend.bangbangbwa.domain.meeting.entity.MeetingStatus;
import com.backend.bangbangbwa.domain.session.entity.RtcSession;
import com.backend.bangbangbwa.domain.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SignalingAccessService {

    private final SessionRepository sessionRepository;

    public void validateAccess(
            String roomToken,
            Long userId
    ) {
        RtcSession rtcSession =
                sessionRepository.findByRoomToken(roomToken)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.SESSION_NOT_FOUND
                                )
                        );

        if (rtcSession.isEnded()) {
            throw new CustomException(
                    ErrorCode.SESSION_ALREADY_ENDED
            );
        }

        if (rtcSession.isExpired(LocalDateTime.now())) {
            throw new CustomException(
                    ErrorCode.SESSION_EXPIRED
            );
        }

        Meeting meeting = rtcSession.getMeeting();

        if (meeting.getStatus() != MeetingStatus.CONFIRMED) {
            throw new CustomException(
                    ErrorCode.INVALID_MEETING_STATUS
            );
        }

        if (!meeting.isParticipant(userId)) {
            throw new CustomException(
                    ErrorCode.SESSION_ACCESS_DENIED
            );
        }
    }
}
