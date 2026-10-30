package com.backend.bangbangbwa.domain.session.dto;

import com.backend.bangbangbwa.domain.session.entity.RtcSession;
import com.backend.bangbangbwa.domain.session.entity.SessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionJoinResponse {

    private Long sessionId;
    private Long meetingId;
    private String roomToken;
    private String signalingUrl;
    private SessionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;

    public static SessionJoinResponse from(
            RtcSession session,
            String signalingUrl
    ) {
        return SessionJoinResponse.builder()
                .sessionId(session.getSessionId())
                .meetingId(session.getMeeting().getId())
                .roomToken(session.getRoomToken())
                .signalingUrl(signalingUrl)
                .status(session.getStatus())
                .startedAt(session.getStartedAt())
                .expiresAt(session.getExpiresAt())
                .build();
    }
}
