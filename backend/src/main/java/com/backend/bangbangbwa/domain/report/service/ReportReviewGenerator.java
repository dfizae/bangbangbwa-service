package com.backend.bangbangbwa.domain.report.service;

import com.backend.bangbangbwa.domain.checklist.dto.ChecklistResponse;
import com.backend.bangbangbwa.domain.report.dto.CaptureResponse;

import java.util.List;

/**
 * 캡쳐 이미지 + 체크리스트 + 하자 정보를 바탕으로 AI 총평을 생성하는 포트.
 *
 * <p>구현체(예: OpenAI)는 이미지와 체크리스트를 모델에 전달해 임차인용 총평 문장을 반환합니다.
 * AI 연동은 리포트 생성의 부가 기능이므로, 생성에 실패하거나 비활성화된 경우 {@code null}을
 * 반환하면 총평 없이 리포트가 완료됩니다.
 */
public interface ReportReviewGenerator {

    /**
     * @param propertyTitle 매물 제목(없으면 null)
     * @param checklist     체크리스트(없으면 null)
     * @param captures      캡쳐 이미지 및 하자 목록
     * @return 생성된 총평 텍스트, 생성하지 못하면 null
     */
    String generateReview(
            String propertyTitle,
            ChecklistResponse checklist,
            List<CaptureResponse> captures
    );
}
