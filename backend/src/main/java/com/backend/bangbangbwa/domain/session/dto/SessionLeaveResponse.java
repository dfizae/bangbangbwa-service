package com.backend.bangbangbwa.domain.session.dto;

import com.backend.bangbangbwa.domain.session.entity.SessionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionLeaveResponse {

    private Long sessionId;
    private SessionStatus status;
    private LocalDateTime leftAt;
}
