package com.backend.bangbangbwa.domain.session.dto;

import com.backend.bangbangbwa.domain.session.entity.RtcSession;
import com.backend.bangbangbwa.domain.session.entity.SessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionCreateResponse {

    private Long sessionId;
    private Long meetingId;
    private String roomToken;
    private String joinUrl;
    private SessionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public static SessionCreateResponse from(
            RtcSession session,
            String joinUrl
    ) {
        return SessionCreateResponse.builder()
                .sessionId(session.getSessionId())
                .meetingId(session.getMeeting().getId())
                .roomToken(session.getRoomToken())
                .joinUrl(joinUrl)
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .expiresAt(session.getExpiresAt())
                .build();
    }
}
