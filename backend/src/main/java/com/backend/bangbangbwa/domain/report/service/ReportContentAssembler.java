package com.backend.bangbangbwa.domain.report.service;

import com.backend.bangbangbwa.domain.checklist.dto.ChecklistResponse;
import com.backend.bangbangbwa.domain.checklist.entity.Checklist;
import com.backend.bangbangbwa.domain.checklist.entity.ChecklistItem;
import com.backend.bangbangbwa.domain.checklist.repository.ChecklistItemRepository;
import com.backend.bangbangbwa.domain.checklist.repository.ChecklistRepository;
import com.backend.bangbangbwa.domain.report.dto.CaptureResponse;
import com.backend.bangbangbwa.domain.report.dto.ReportDetailResponse;
import com.backend.bangbangbwa.domain.report.entity.Report;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 리포트 상세 데이터(체크리스트 + 캡쳐/하자)를 조립합니다.
 * 상세 조회와 PDF 생성에서 공통으로 사용됩니다.
 *
 * <p>캡쳐/하자 데이터는 detection 모듈이 구현하는 {@link ReportCaptureProvider} 포트에서
 * 가져오므로, 리포트 도메인은 detection 내부 엔티티에 의존하지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class ReportContentAssembler {

    private final ChecklistRepository checklistRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ReportCaptureProvider captureProvider;

    public ReportDetailResponse assemble(Report report) {
        Long meetingId = report.getMeeting().getId();

        ChecklistResponse checklist = loadChecklist(meetingId);
        List<CaptureResponse> captures =
                captureProvider.getCapturesByMeetingId(meetingId);

        return ReportDetailResponse.of(report, checklist, captures);
    }

    private ChecklistResponse loadChecklist(Long meetingId) {
        return checklistRepository.findByMeeting_Id(meetingId)
                .map(this::toChecklistResponse)
                .orElse(null);
    }

    private ChecklistResponse toChecklistResponse(Checklist checklist) {
        List<ChecklistItem> items =
                checklistItemRepository
                        .findAllByChecklist_IdOrderByIdAsc(checklist.getId());

        return ChecklistResponse.from(checklist, items);
    }
}
