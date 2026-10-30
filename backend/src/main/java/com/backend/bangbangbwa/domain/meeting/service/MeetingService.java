package com.backend.bangbangbwa.domain.meeting.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.checklist.entity.Checklist;
import com.backend.bangbangbwa.domain.checklist.repository.ChecklistItemRepository;
import com.backend.bangbangbwa.domain.checklist.repository.ChecklistRepository;
import com.backend.bangbangbwa.domain.meeting.dto.MeetingCreateRequest;
import com.backend.bangbangbwa.domain.meeting.dto.MeetingPreferredTimesUpdateRequest;
import com.backend.bangbangbwa.domain.meeting.dto.MeetingResponse;
import com.backend.bangbangbwa.domain.meeting.entity.Meeting;
import com.backend.bangbangbwa.domain.meeting.entity.MeetingStatus;
import com.backend.bangbangbwa.domain.meeting.repository.MeetingRepository;
import com.backend.bangbangbwa.domain.property.entity.Property;
import com.backend.bangbangbwa.domain.property.repository.PropertyRepository;
import com.backend.bangbangbwa.domain.user.entity.User;
import com.backend.bangbangbwa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final MeetingCleanupService meetingCleanupService;

    @Transactional
    public MeetingResponse createMeetings(
            Long tenantId,
            Long propertyId,
            MeetingCreateRequest request
    ) {
        User tenant = userRepository
                .findByIdAndDeletedAtIsNull(tenantId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );

        if (!tenant.isTenant()) {
            throw new CustomException(
                    ErrorCode.PROPERTY_ACCESS_DENIED
            );
        }

        Property property = propertyRepository
                .findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.PROPERTY_NOT_FOUND
                        )
                );

        Long agentId = property.getRegistrant().getId();

        if (tenant.getId().equals(agentId)) {
            throw new CustomException(
                    ErrorCode.MEETING_SAME_PARTICIPANT
            );
        }

        List<LocalDateTime> preferredTimes =
                request.getPreferredTimes();

        // 기존에 만들어둔 공통 검증 메서드 사용
        validatePreferredTimes(preferredTimes);

        /*
         * confirmMeeting()과 같은 중개인 행을 잠근다.
         *
         * 이 락을 획득한 이후에 확정 일정 충돌 여부를 조회해야
         * 생성과 확정이 동시에 실행되는 경우를 제어할 수 있다.
         */
        User agent = userRepository
                .findAgentForScheduleUpdate(agentId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );

        // 해당 매물에 이미 확정된 시간이 있는지 확인
        boolean confirmedTimeExists =
                meetingRepository
                        .existsByPropertyIdAndScheduledAtInAndStatusAndDeletedAtIsNull(
                                propertyId,
                                preferredTimes,
                                MeetingStatus.CONFIRMED
                        );

        if (confirmedTimeExists) {
            throw new CustomException(
                    ErrorCode.MEETING_SCHEDULE_CONFLICT
            );
        }

        // 같은 임차인의 동일 매물 활성 요청 확인
        boolean activeMeetingExists =
                meetingRepository
                        .existsByPropertyIdAndTenant_IdAndStatusInAndDeletedAtIsNull(
                                propertyId,
                                tenantId,
                                List.of(
                                        MeetingStatus.REQUESTED,
                                        MeetingStatus.CONFIRMED
                                )
                        );

        if (activeMeetingExists) {
            throw new CustomException(
                    ErrorCode.MEETING_REQUEST_ALREADY_EXISTS
            );
        }

        LocalDateTime preferredAt1 =
                preferredTimes.get(0);

        LocalDateTime preferredAt2 =
                preferredTimes.size() >= 2
                        ? preferredTimes.get(1)
                        : null;

        LocalDateTime preferredAt3 =
                preferredTimes.size() >= 3
                        ? preferredTimes.get(2)
                        : null;

        Meeting meeting = Meeting.builder()
                .propertyId(propertyId)
                .tenant(tenant)
                .agent(agent)
                .preferredAt1(preferredAt1)
                .preferredAt2(preferredAt2)
                .preferredAt3(preferredAt3)
                .build();

        Meeting savedMeeting =
                meetingRepository.save(meeting);

        return MeetingResponse.from(savedMeeting);
    }

    @Transactional
    public MeetingResponse updatePreferredTimes(
            Long tenantId,
            Long meetingId,
            MeetingPreferredTimesUpdateRequest request
    ) {
        List<LocalDateTime> preferredTimes =
                request.getPreferredTimes();

        // DB 락을 잡기 전에 요청값부터 검증
        validatePreferredTimes(preferredTimes);

        // 1. 중개인 User 락
        userRepository
                .findAgentForMeetingScheduleUpdate(
                        meetingId,
                        tenantId
                )
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.MEETING_NOT_FOUND
                        )
                );

        // 2. Meeting 락
        Meeting meeting = meetingRepository
                .findByIdAndTenantId(
                        meetingId,
                        tenantId
                )
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.MEETING_NOT_FOUND
                        )
                );

        validateStatus(
                meeting,
                MeetingStatus.REQUESTED
        );

        // 수정하려는 시간 중 이미 확정된 시간이 있는지 확인
        boolean confirmedTimeExists =
                meetingRepository
                        .existsByPropertyIdAndScheduledAtInAndStatusAndDeletedAtIsNull(
                                meeting.getPropertyId(),
                                preferredTimes,
                                MeetingStatus.CONFIRMED
                        );

        if (confirmedTimeExists) {
            throw new CustomException(
                    ErrorCode.MEETING_SCHEDULE_CONFLICT
            );
        }

        meeting.updatePreferredTimes(
                preferredTimes.get(0),
                preferredTimes.size() >= 2
                        ? preferredTimes.get(1)
                        : null,
                preferredTimes.size() >= 3
                        ? preferredTimes.get(2)
                        : null
        );

        meetingRepository.flush();

        return MeetingResponse.from(meeting);
    }


    @Transactional
    public MeetingResponse confirmMeeting(
            Long agentId,
            Long meetingId,
            int preferredRank
    ) {

        User agent = userRepository
                .findAgentForScheduleUpdate(agentId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        if (!agent.isAgent()) {
            throw new CustomException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }

        Meeting meeting = getMeetingForAgent(
                meetingId,
                agentId
        );

        validateStatus(
                meeting,
                MeetingStatus.REQUESTED
        );

        User tenant = userRepository
                .findUserForScheduleUpdate(
                        meeting.getTenant().getId()
                )
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        LocalDateTime scheduledAt = switch (preferredRank) {
            case 1 -> meeting.getPreferredAt1();
            case 2 -> meeting.getPreferredAt2();
            case 3 -> meeting.getPreferredAt3();
            default -> throw new CustomException(ErrorCode.INVALID_REQUEST);
        };

        if (scheduledAt == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        boolean agentTimeConflict =
                meetingRepository
                        .existsByAgent_IdAndScheduledAtAndStatusAndDeletedAtIsNull(
                                agentId,
                                scheduledAt,
                                MeetingStatus.CONFIRMED
                        );

        boolean tenantTimeConflict =
                meetingRepository
                        .existsByTenant_IdAndScheduledAtAndStatusAndDeletedAtIsNull(
                                tenant.getId(),
                                scheduledAt,
                                MeetingStatus.CONFIRMED
                        );

        if (agentTimeConflict || tenantTimeConflict) {
            throw new CustomException(
                    ErrorCode.MEETING_SCHEDULE_CONFLICT
            );
        }

        //확정하려는 시간이 이미 과거 시간이라면?
        if(!scheduledAt.isAfter(LocalDateTime.now())){
            throw new CustomException(
                    ErrorCode.INVALID_MEETING_DATE
            );
        }

        meeting.confirm(scheduledAt);

        Checklist checklist = Checklist.builder()
                .meeting(meeting)
                .title("입주 체크리스트") // 이 부분 추후에 매물 이름 포함하도록 할 수 있음.
                .build();

        checklistRepository.save(checklist);

        return MeetingResponse.from(meeting);
    }

    //예약 거절
    @Transactional
    public MeetingResponse rejectMeeting(
            Long agentId,
            Long meetingId
    ) {

        Meeting meeting = getMeetingForAgent(
                meetingId,
                agentId
        );

        validateStatus(
                meeting,
                MeetingStatus.REQUESTED
        );

        meeting.reject();

        return MeetingResponse.from(meeting);
    }

    //취소인데 확정된 회의 취소 & 확정되지 않은 회의 취소. 두개로 나눠야 할 수도 있음.
    @Transactional
    public void cancelMeeting(
            Long userId,
            Long meetingId
    ) {
        Meeting meeting = getAccessibleMeeting(
                meetingId,
                userId
        );
        switch (meeting.getStatus()) {
            case REQUESTED -> {
                meeting.cancel();
            }

            case CONFIRMED -> {
                validateCancellationDeadline(meeting);
                deleteChecklist(meeting.getId());
                meeting.cancel();
            }

            case CANCELED -> throw new CustomException(
                    ErrorCode.MEETING_ALREADY_CANCELED
            );

            case COMPLETED, REJECTED, OPEN -> throw new CustomException(
                    ErrorCode.INVALID_MEETING_STATUS
            );
        }
    }

    //완료된 회의 삭제(소프트 삭제 + 연관 데이터 정리)
    @Transactional
    public void deleteCompletedMeeting(
            Long userId,
            Long meetingId
    ) {
        // 락 없이 참여자 권한 + 상태 검증
        // (cleanupMeeting 이 REQUIRES_NEW 라 비관적 락과 겹치면 데드락 소지가 있어 락을 잡지 않는다)
        Meeting meeting = meetingRepository
                .findMeetingDetail(
                        meetingId,
                        userId
                )
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.MEETING_NOT_FOUND
                        )
                );

        validateStatus(
                meeting,
                MeetingStatus.COMPLETED
        );

        // 스케줄러와 동일한 정리 로직 재사용: 세션/체크리스트 삭제 후 deletedAt 세팅
        meetingCleanupService.cleanupMeeting(meetingId);
    }

    //매물에 요청된 회의 예약 확인(중개인용)
    public Page<MeetingResponse> getRequestedMeetings(
            Long propertyId,
            Long userId,
            Pageable pageable
    ) {

        //매물을 등록한 중개인만 확인 가능.
        Property property = propertyRepository
                .findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROPERTY_NOT_FOUND));

        if (!property.getRegistrant().getId().equals(userId)) {
            throw new CustomException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }

        return meetingRepository
                .findAllRequestedByPropertyId(
                        propertyId,
                        MeetingStatus.REQUESTED,
                        pageable
                )
                .map(MeetingResponse::from);
    }

    public List<LocalDateTime> getUnavailableMeetingTimes(
            Long tenantId,
            Long propertyId,
            LocalDateTime from,
            LocalDateTime to
    ) {

        getActiveUser(tenantId);

        Property property = propertyRepository
                .findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.PROPERTY_NOT_FOUND)
                );

        Long agentId = property.getRegistrant().getId();

        if (!from.isBefore(to) || from.plusDays(31).isBefore(to)) {
            throw new CustomException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        return meetingRepository
                .findUnavailableTimesForAgentAndProperty(
                        agentId,
                        from,
                        to,
                        MeetingStatus.CONFIRMED
                );
    }

    public Page<MeetingResponse> getMeetings(
            Long userId,
            boolean includeCompleted,
            boolean includeCanceled,
            Pageable pageable
    ) {
        getActiveUser(userId);

        return meetingRepository
                .findAllByParticipantId(
                        userId,
                        includeCompleted,
                        MeetingStatus.COMPLETED,
                        includeCanceled,
                        MeetingStatus.CANCELED,
                        pageable
                )
                .map(MeetingResponse::from);
    }

    public Page<MeetingResponse> getPropertyMeetingsForAgent(
            Long agentId,
            Long propertyId,
            Pageable pageable
    ) {
        getActiveUser(agentId);

        return meetingRepository
                .findAllByPropertyIdAndAgentId(
                        propertyId,
                        agentId,
                        MeetingStatus.CONFIRMED,
                        pageable
                )
                .map(MeetingResponse::from);
    }

    public MeetingResponse getMeetingDetail(
            Long userId,
            Long meetingId
    ) {
        Meeting meeting = meetingRepository
                .findMeetingDetail(
                        meetingId,
                        userId
                )
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.MEETING_NOT_FOUND
                        )
                );

        return MeetingResponse.from(meeting);
    }

    private Meeting getAccessibleMeeting(
            Long meetingId,
            Long userId
    ) {
        return meetingRepository
                .findAccessibleMeeting(
                        meetingId,
                        userId
                )
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.MEETING_NOT_FOUND
                        )
                );
    }

    private Meeting getMeetingForAgent(
            Long meetingId,
            Long agentId
    ) {
        return meetingRepository
                .findByIdAndAgentId(
                        meetingId,
                        agentId
                )
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.MEETING_NOT_FOUND
                        )
                );
    }

    private User getActiveUser(Long userId) {
        return userRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );
    }

    private void validateStatus(
            Meeting meeting,
            MeetingStatus expectedStatus
    ) {
        if (meeting.getStatus() != expectedStatus) {
            throw new CustomException(
                    ErrorCode.INVALID_MEETING_STATUS
            );
        }
    }

    private void validatePreferredTimes(
            List<LocalDateTime> preferredTimes
    ) {
        if (preferredTimes == null
                || preferredTimes.isEmpty()
                || preferredTimes.size() > 3) {
            throw new CustomException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        if (new HashSet<>(preferredTimes).size()
                != preferredTimes.size()) {
            throw new CustomException(
                    ErrorCode.DUPLICATE_PREFERRED_TIME
            );
        }

        LocalDateTime now = LocalDateTime.now();

        for (LocalDateTime preferredAt : preferredTimes) {
            if (preferredAt == null
                    || !preferredAt.isAfter(now)) {
                throw new CustomException(
                        ErrorCode.INVALID_MEETING_DATE
                );
            }

            boolean validMinute =
                    preferredAt.getMinute() == 0
                            || preferredAt.getMinute() == 30;

            boolean validSecond =
                    preferredAt.getSecond() == 0
                            && preferredAt.getNano() == 0;

            if (!validMinute || !validSecond) {
                throw new CustomException(
                        ErrorCode.INVALID_MEETING_TIME_SLOT
                );
            }
        }
    }

    private void validateCancellationDeadline(
            Meeting meeting
    ) {
        LocalDateTime cancellationDeadline =
                meeting.getScheduledAt().minusHours(3);

        if (LocalDateTime.now().isAfter(cancellationDeadline)) {
            throw new CustomException(
                    ErrorCode.MEETING_CANCEL_DEADLINE_PASSED
            );
        }
    }

    private void deleteChecklist(Long meetingId) {
        checklistRepository
                .findByMeeting_Id(meetingId)
                .ifPresent(checklist -> {
                    checklistItemRepository
                            .deleteAllByChecklist_Id(checklist.getId());

                    checklistRepository.delete(checklist);
                });
    }
}
