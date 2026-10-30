package com.backend.bangbangbwa.domain.meeting.service;

import com.backend.bangbangbwa.domain.checklist.repository.ChecklistItemRepository;
import com.backend.bangbangbwa.domain.checklist.repository.ChecklistRepository;
import com.backend.bangbangbwa.domain.detection.entity.SessionCapture;
import com.backend.bangbangbwa.domain.detection.repository.SessionCaptureRepository;
import com.backend.bangbangbwa.domain.meeting.entity.Meeting;
import com.backend.bangbangbwa.domain.meeting.repository.MeetingRepository;
import com.backend.bangbangbwa.domain.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingCleanupService {

    private final MeetingRepository meetingRepository;
    private final SessionRepository sessionRepository;
    private final SessionCaptureRepository sessionCaptureRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistItemRepository checklistItemRepository;

    /**
     * 보관 기간이 지난 종료 회의 한 건과 연관 데이터를 정리한다.
     *
     * <p>연관 데이터(session_capture/defect, checklist_item/checklist, session)는
     * 실제로 삭제해 저장 공간을 회수하고, 회의 자체는 소프트 삭제(deletedAt)로
     * 목록/상세 조회에서 제외한다.</p>
     *
     * <p>회의별로 독립된 트랜잭션(REQUIRES_NEW)에서 처리하므로,
     * 한 건이 실패해도 다른 회의의 정리에는 영향을 주지 않는다.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupMeeting(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElse(null);

        if (meeting == null || meeting.getDeletedAt() != null) {
            return;
        }

        deleteSessionData(meetingId);
        deleteChecklistData(meetingId);

        meeting.softDelete(LocalDateTime.now());
    }

    private void deleteSessionData(Long meetingId) {
        sessionRepository.findByMeeting_Id(meetingId)
                .ifPresent(session -> {
                    List<SessionCapture> captures =
                            sessionCaptureRepository
                                    .findBySession_SessionId(
                                            session.getSessionId()
                                    );

                    /*
                     * SessionCapture 삭제 시 cascade/orphanRemoval 로
                     * 연관된 defect 가 함께 삭제된다.
                     * 세션보다 먼저 지우기 위해 flush 로 순서를 보장한다.
                     */
                    sessionCaptureRepository.deleteAll(captures);
                    sessionCaptureRepository.flush();

                    sessionRepository.delete(session);
                    sessionRepository.flush();
                });
    }

    private void deleteChecklistData(Long meetingId) {
        checklistRepository.findByMeeting_Id(meetingId)
                .ifPresent(checklist -> {
                    checklistItemRepository
                            .deleteAllByChecklist_Id(checklist.getId());

                    checklistRepository.delete(checklist);
                });
    }
}
