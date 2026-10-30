package com.backend.bangbangbwa.domain.session.dto;

import com.backend.bangbangbwa.domain.session.entity.RtcSession;
import com.backend.bangbangbwa.domain.session.entity.SessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionEndResponse {

    private Long sessionId;
    private Long meetingId;
    private SessionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public static SessionEndResponse from(RtcSession session) {
        return SessionEndResponse.builder()
                .sessionId(session.getSessionId())
                .meetingId(session.getMeeting().getId())
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .build();
    }
}
