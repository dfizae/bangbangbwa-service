package com.backend.bangbangbwa.domain.meeting.dto;

import com.backend.bangbangbwa.domain.meeting.entity.Meeting;
import com.backend.bangbangbwa.domain.meeting.entity.MeetingStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MeetingResponse {

    private Long meetingId;
    private Long propertyId;
    private Long tenantId;
    private String tenantNickname;
    private Long agentId;
    private String agentNickname;
    private LocalDateTime preferredAt1;
    private LocalDateTime preferredAt2;
    private LocalDateTime preferredAt3;
    private LocalDateTime scheduledAt;
    private MeetingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MeetingResponse from(Meeting meeting) {
        return new MeetingResponse(
                meeting.getId(),
                meeting.getPropertyId(),
                meeting.getTenant() != null ? meeting.getTenant().getId() : null,
                meeting.getTenant() != null ? meeting.getTenant().getNickname() : null,
                meeting.getAgent().getId(),
                meeting.getAgent().getNickname(),
                meeting.getPreferredAt1(),
                meeting.getPreferredAt2(),
                meeting.getPreferredAt3(),
                meeting.getScheduledAt(),
                meeting.getStatus(),
                meeting.getCreatedAt(),
                meeting.getUpdatedAt()
        );
    }
}
