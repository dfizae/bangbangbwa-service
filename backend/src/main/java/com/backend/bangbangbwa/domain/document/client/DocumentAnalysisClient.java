package com.backend.bangbangbwa.domain.document.client;

import com.backend.bangbangbwa.domain.document.entity.DocumentType;
import com.backend.bangbangbwa.domain.document.entity.RiskLevel;

/**
 * 매물 서류(PDF)를 AI로 분석해 권리관계 위험도를 판정하는 클라이언트.
 */
public interface DocumentAnalysisClient {

    AnalysisResult analyze(
            String documentUrl,
            DocumentType documentType,
            DocumentContext context
    );

    /** 등기부 내용과 대조할 매물 정보 */
    record DocumentContext(String address) {
    }

    /** AI 분석 결과 */
    record AnalysisResult(
            RiskLevel riskLevel,
            String summary,
            String resultJson
    ) {
    }
}
