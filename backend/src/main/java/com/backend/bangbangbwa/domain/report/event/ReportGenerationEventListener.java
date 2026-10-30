package com.backend.bangbangbwa.domain.report.event;

import com.backend.bangbangbwa.config.AsyncConfig;
import com.backend.bangbangbwa.domain.report.service.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 리포트 생성 요청 커밋 이후, 별도 스레드/트랜잭션에서 비동기로 리포트를 조립합니다.
 *
 * <p>조립이 실패하면 한 번 더 시도하고(각 시도는 독립 트랜잭션), 재시도까지 실패하면
 * 리포트를 REJECTED로 확정합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportGenerationEventListener {

    /** 최초 1회 + 재시도 1회 */
    private static final int MAX_ATTEMPTS = 2;

    private final ReportGenerationService reportGenerationService;

    @Async(AsyncConfig.REPORT_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReportGenerationRequested(
            ReportGenerationRequestedEvent event
    ) {
        Long reportId = event.reportId();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                reportGenerationService.generate(reportId);
                return;
            } catch (Exception exception) {
                log.warn(
                        "리포트 조립 실패 (시도 {}/{}). reportId={}",
                        attempt, MAX_ATTEMPTS, reportId, exception
                );
            }
        }

        log.error(
                "리포트 조립이 재시도까지 실패하여 REJECTED 처리합니다. reportId={}",
                reportId
        );
        reportGenerationService.markRejected(reportId);
    }
}
