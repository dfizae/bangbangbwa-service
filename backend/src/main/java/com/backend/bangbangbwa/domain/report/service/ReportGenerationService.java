package com.backend.bangbangbwa.domain.report.service;

import com.backend.bangbangbwa.domain.property.entity.Property;
import com.backend.bangbangbwa.domain.property.repository.PropertyRepository;
import com.backend.bangbangbwa.domain.report.dto.ReportDetailResponse;
import com.backend.bangbangbwa.domain.report.entity.Report;
import com.backend.bangbangbwa.domain.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리포트 비동기 조립 로직.
 *
 * <p>체크리스트/캡쳐/하자 데이터를 모아 AI 총평({@link ReportReviewGenerator})을 생성해
 * 리포트에 저장하고 CONFIRMED로 확정합니다.
 *
 * <p>조립 실패 시 상태를 직접 바꾸지 않고 예외를 전파하여, 상위 리스너가 재시도할 수 있게 합니다.
 * 재시도까지 모두 실패한 경우에만 {@link #markRejected(Long)}로 REJECTED 처리합니다.
 * AI 총평은 부가 기능이라, 총평 생성기가 비활성(API 키 없음 등)이면 총평 없이 완료하되,
 * 생성기 <b>호출이 실패</b>하면 리포트 생성 자체를 실패로 간주해 재시도 대상으로 둡니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private final ReportRepository reportRepository;
    private final ReportContentAssembler reportContentAssembler;
    private final PropertyRepository propertyRepository;
    private final ObjectProvider<ReportReviewGenerator> reviewGenerator;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generate(Long reportId) {
        Report report = reportRepository.findWithMeetingById(reportId).orElse(null);

        if (report == null) {
            log.warn("리포트 조립 대상이 존재하지 않습니다. reportId={}", reportId);
            return;
        }

        ReportDetailResponse detail = reportContentAssembler.assemble(report);
        String propertyTitle = resolvePropertyTitle(report.getMeeting().getPropertyId());

        String review = generateReview(propertyTitle, detail);
        if (review != null) {
            report.applySummary(review);
        }

        report.markConfirmed();
        reportRepository.save(report);
    }

    /**
     * 재시도까지 모두 실패한 리포트를 REJECTED로 확정합니다. 독립 트랜잭션에서 수행합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRejected(Long reportId) {
        reportRepository.findById(reportId).ifPresent(report -> {
            report.markRejected();
            reportRepository.save(report);
        });
    }

    private String resolvePropertyTitle(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .map(Property::getTitle)
                .orElse(null);
    }

    /**
     * AI 총평을 생성합니다.
     * 생성기가 없거나 비활성이면 {@code null}을 반환해 총평 없이 완료되게 하고,
     * 생성기 호출 중 발생한 예외는 그대로 전파해 재시도로 이어지게 합니다.
     */
    private String generateReview(
            String propertyTitle,
            ReportDetailResponse detail
    ) {
        ReportReviewGenerator generator = reviewGenerator.getIfAvailable();

        if (generator == null) {
            return null;
        }

        return generator.generateReview(
                propertyTitle,
                detail.getChecklist(),
                detail.getCaptures()
        );
    }
}
