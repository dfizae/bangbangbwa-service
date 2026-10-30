package com.backend.bangbangbwa.domain.report.event;

/**
 * 리포트 생성 요청이 커밋된 후 비동기 조립을 트리거하는 이벤트.
 */
public record ReportGenerationRequestedEvent(Long reportId) {
}
