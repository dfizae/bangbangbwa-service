package com.backend.bangbangbwa.domain.report.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.meeting.entity.Meeting;
import com.backend.bangbangbwa.domain.property.entity.Property;
import com.backend.bangbangbwa.domain.property.entity.PropertyImage;
import com.backend.bangbangbwa.domain.property.repository.PropertyImageRepository;
import com.backend.bangbangbwa.domain.property.repository.PropertyRepository;
import com.backend.bangbangbwa.domain.property.storage.StorageService;
import com.backend.bangbangbwa.domain.report.dto.CaptureResponse;
import com.backend.bangbangbwa.domain.report.dto.ReportDetailResponse;
import com.backend.bangbangbwa.domain.report.dto.ReportStatusResponse;
import com.backend.bangbangbwa.domain.report.dto.ReportSummaryResponse;
import com.backend.bangbangbwa.domain.report.entity.Report;
import com.backend.bangbangbwa.domain.report.event.ReportGenerationRequestedEvent;
import com.backend.bangbangbwa.domain.report.repository.ReportRepository;
import com.backend.bangbangbwa.domain.session.entity.RtcSession;
import com.backend.bangbangbwa.domain.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final DateTimeFormatter FILE_NAME_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(60);

    private final ReportRepository reportRepository;
    private final SessionRepository sessionRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final ReportContentAssembler reportContentAssembler;
    private final StorageService storageService;
    private final ReportPdfGenerator reportPdfGenerator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 세션 종료 후 리포트 생성
     * 동일 미팅(=세션)에 대해 이미 리포트가 있으면 생성하지 않고 예외를 반환한다.
     *
     * <p>중복 여부는 {@code ON CONFLICT DO NOTHING} 으로 DB에서 원자적으로 판정하므로
     * 동시 요청에도 정확히 한 건만 생성되고, 나머지는 {@code REPORT_ALREADY_EXISTS} 로 처리된다.
     */
    @Transactional
    public ReportStatusResponse createReport(
            Long sessionId,
            Long currentUserId
    ) {
        RtcSession session = getSession(sessionId);
        Meeting meeting = session.getMeeting();

        validateParticipant(meeting, currentUserId);

        if (!session.isEnded()) {
            throw new CustomException(ErrorCode.SESSION_NOT_ENDED);
        }

        int inserted = reportRepository.insertIfAbsent(meeting.getId());

        if (inserted == 0) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_EXISTS);
        }

        Report report = reportRepository.findByMeeting_Id(meeting.getId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.REPORT_GENERATION_FAILED)
                );

        eventPublisher.publishEvent(
                new ReportGenerationRequestedEvent(report.getReportId())
        );

        return ReportStatusResponse.from(report);
    }

    public ReportStatusResponse getReportStatus(
            Long reportId,
            Long currentUserId
    ) {
        Report report = getReport(reportId);

        validateParticipant(report.getMeeting(), currentUserId);

        return ReportStatusResponse.from(report);
    }

    public ReportDetailResponse getReportDetail(
            Long reportId,
            Long currentUserId
    ) {
        Report report = getReport(reportId);

        validateParticipant(report.getMeeting(), currentUserId);

        ReportDetailResponse detail = reportContentAssembler.assemble(report);
        return withPresignedCaptureUrls(report, detail);
    }

    public Page<ReportSummaryResponse> getMyReports(
            Long currentUserId,
            Pageable pageable
    ) {
        Page<Report> reports =
                reportRepository.findAllByParticipantId(currentUserId, pageable);

        List<Long> propertyIds = reports.getContent().stream()
                .map(report -> report.getMeeting().getPropertyId())
                .distinct()
                .toList();

        Map<Long, String> titleByPropertyId = loadTitles(propertyIds);
        Map<Long, String> thumbnailByPropertyId = loadThumbnails(propertyIds);

        return reports.map(report -> {
            Long propertyId = report.getMeeting().getPropertyId();
            return ReportSummaryResponse.of(
                    report,
                    titleByPropertyId.get(propertyId),
                    thumbnailByPropertyId.get(propertyId)
            );
        });
    }

    public ReportPdfContent downloadReport(
            Long reportId,
            Long currentUserId
    ) {
        Report report = getReport(reportId);

        validateParticipant(report.getMeeting(), currentUserId);

        if (!report.isConfirmed()) {
            throw new CustomException(ErrorCode.REPORT_NOT_COMPLETED);
        }

        ReportDetailResponse detail = reportContentAssembler.assemble(report);

        String propertyTitle = propertyRepository
                .findById(report.getMeeting().getPropertyId())
                .map(Property::getTitle)
                .orElse(null);

        byte[] pdf = reportPdfGenerator.generate(detail, propertyTitle);

        return new ReportPdfContent(
                buildFileName(report, propertyTitle),
                pdf
        );
    }

    @Transactional
    public void deleteReport(
            Long reportId,
            Long currentUserId
    ) {
        Report report = getReport(reportId);

        validateTenant(report.getMeeting(), currentUserId);

        reportRepository.delete(report);
    }

    /**
     * 다운로드 파일명을 {@code {매물명}_점검리포트_{yyyyMMdd}.pdf} 형식으로 만든다.
     * 매물명이 없으면 리포트 번호로 대체하고, 파일명에 부적합한 문자는 밑줄로 치환한다.
     */
    private String buildFileName(Report report, String propertyTitle) {
        String name = (propertyTitle == null || propertyTitle.isBlank())
                ? "리포트" + report.getReportId()
                : propertyTitle.trim();

        String safeName = name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");

        String date = report.getCreatedAt() == null
                ? ""
                : "_" + report.getCreatedAt().format(FILE_NAME_DATE_FORMATTER);

        return safeName + "_점검리포트" + date + ".pdf";
    }

    private Map<Long, String> loadTitles(List<Long> propertyIds) {
        if (propertyIds.isEmpty()) {
            return Map.of();
        }

        return propertyRepository.findAllById(propertyIds).stream()
                .filter(property -> property.getTitle() != null)
                .collect(Collectors.toMap(
                        Property::getId,
                        Property::getTitle
                ));
    }

    private Map<Long, String> loadThumbnails(List<Long> propertyIds) {
        if (propertyIds.isEmpty()) {
            return Map.of();
        }

        return propertyImageRepository
                .findByPropertyIdsOrderBySequence(propertyIds).stream()
                .collect(Collectors.toMap(
                        image -> image.getProperty().getId(),
                        PropertyImage::getImageUrl,
                        (first, second) -> first
                ));
    }

    private RtcSession getSession(Long sessionId) {
        return sessionRepository.findWithMeetingById(sessionId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.SESSION_NOT_FOUND)
                );
    }

    /**
     * 상세 조회 응답의 캡쳐 이미지 URL을 presigned URL로 교체합니다. 저장된 원본
     * S3 키는 비공개 버킷이라 그대로는 접근이 안 되므로 클라이언트가 바로 로드할 수
     * 있게 서명된 GET URL로 바꿉니다. (리포트 생성/PDF 경로는 원본 키가 필요해
     * 공유 assemble은 건드리지 않고 이 조회 응답에서만 변환합니다.)
     */
    private ReportDetailResponse withPresignedCaptureUrls(
            Report report,
            ReportDetailResponse detail
    ) {
        List<CaptureResponse> captures = detail.getCaptures();
        if (captures == null || captures.isEmpty()) {
            return detail;
        }
        List<CaptureResponse> presignedCaptures = captures.stream()
                .map(this::withPresignedImageUrl)
                .toList();
        return ReportDetailResponse.of(
                report,
                detail.getChecklist(),
                presignedCaptures
        );
    }

    private CaptureResponse withPresignedImageUrl(CaptureResponse capture) {
        String imageUrl = capture.getImageUrl();
        if (!StringUtils.hasText(imageUrl)) {
            return capture;
        }
        return CaptureResponse.builder()
                .captureId(capture.getCaptureId())
                .imageUrl(storageService.createPresignedGetUrl(imageUrl, PRESIGNED_URL_TTL))
                .capturedAt(capture.getCapturedAt())
                .defects(capture.getDefects())
                .build();
    }

    private Report getReport(Long reportId) {
        return reportRepository.findWithMeetingById(reportId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.REPORT_NOT_FOUND)
                );
    }

    private void validateParticipant(Meeting meeting, Long currentUserId) {
        if (!meeting.isParticipant(currentUserId)) {
            throw new CustomException(ErrorCode.REPORT_ACCESS_DENIED);
        }
    }

    private void validateTenant(Meeting meeting, Long currentUserId) {
        if (!meeting.isTenant(currentUserId)) {
            throw new CustomException(ErrorCode.REPORT_ACCESS_DENIED);
        }
    }

    /**
     * PDF 파일명과 바이트를 함께 전달하기 위한 값 객체.
     */
    public record ReportPdfContent(String fileName, byte[] content) {
    }
}
