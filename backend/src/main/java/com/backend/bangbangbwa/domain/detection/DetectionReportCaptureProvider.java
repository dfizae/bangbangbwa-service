package com.backend.bangbangbwa.domain.detection;

import com.backend.bangbangbwa.domain.detection.entity.Defect;
import com.backend.bangbangbwa.domain.detection.entity.SessionCapture;
import com.backend.bangbangbwa.domain.detection.repository.SessionCaptureRepository;
import com.backend.bangbangbwa.domain.report.dto.CaptureResponse;
import com.backend.bangbangbwa.domain.report.dto.DefectResponse;
import com.backend.bangbangbwa.domain.report.service.ReportCaptureProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * report 도메인이 정의한 {@link ReportCaptureProvider} 포트를 detection 모듈이 구현하는 어댑터.
 *
 * <p>미팅 → 세션 → 캡쳐/하자로 이어지는 detection 내부 모델을 report DTO로 변환해 공급합니다.
 * 변환 책임을 이 한 곳에 모아, report 도메인이 detection 엔티티에 의존하지 않도록 합니다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DetectionReportCaptureProvider implements ReportCaptureProvider {

    private static final Comparator<Defect> BY_CREATED_AT =
            Comparator.comparing(Defect::getCreatedAt);

    private final SessionCaptureRepository sessionCaptureRepository;

    @Override
    public List<CaptureResponse> getCapturesByMeetingId(Long meetingId) {
        return sessionCaptureRepository
                .findBySession_Meeting_IdOrderByCapturedAtAsc(meetingId)
                .stream()
                .map(this::toCaptureResponse)
                .toList();
    }

    private CaptureResponse toCaptureResponse(SessionCapture capture) {
        return CaptureResponse.builder()
                .captureId(capture.getId())
                .imageUrl(capture.getImageUrl())
                .capturedAt(capture.getCapturedAt())
                .defects(toDefectResponses(capture))
                .build();
    }

    private List<DefectResponse> toDefectResponses(SessionCapture capture) {
        return capture.getDefects().stream()
                .sorted(BY_CREATED_AT)
                .map(this::toDefectResponse)
                .toList();
    }

    private DefectResponse toDefectResponse(Defect defect) {
        return DefectResponse.builder()
                .defectId(defect.getId())
                .type(name(defect.getType()))
                .confidence(defect.getConfidence())
                .description(defect.getDescription())
                .bboxXCenter(defect.getBboxXCenter())
                .bboxYCenter(defect.getBboxYCenter())
                .bboxWidth(defect.getBboxWidth())
                .bboxHeight(defect.getBboxHeight())
                .status(name(defect.getStatus()))
                .build();
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
