package com.backend.bangbangbwa.domain.document.service;

import com.backend.bangbangbwa.domain.document.client.DocumentAnalysisClient;
import com.backend.bangbangbwa.domain.document.client.DocumentAnalysisClient.AnalysisResult;
import com.backend.bangbangbwa.domain.document.client.DocumentAnalysisClient.DocumentContext;
import com.backend.bangbangbwa.domain.document.entity.Document;
import com.backend.bangbangbwa.domain.document.entity.DocumentAnalysis;
import com.backend.bangbangbwa.domain.document.entity.DocumentType;
import com.backend.bangbangbwa.domain.document.repository.DocumentAnalysisRepository;
import com.backend.bangbangbwa.domain.document.repository.DocumentRepository;
import com.backend.bangbangbwa.domain.document.storage.PropertyDocumentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 업로드된 서류를 AI로 분석해 결과를 저장하는 백그라운드 처리기.
 * 이벤트 리스너(@Async)에서 호출되며, 성공/실패에 따라 document 상태를 갱신한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAnalysisProcessor {

    // AI 서버가 서류를 직접 내려받는 데 사용할 presigned URL 만료 시간
    private static final Duration ANALYSIS_URL_TTL = Duration.ofMinutes(10);

    private final DocumentRepository documentRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final DocumentAnalysisClient analysisClient;
    private final PropertyDocumentStorage documentStorage;

    @Transactional
    public void process(
            Long documentId,
            DocumentType documentType,
            String address
    ) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return;
        }

        // 재분석 대비: 기존 분석 결과 제거 후 새로 저장
        documentAnalysisRepository.deleteByDocumentId(documentId);
        documentAnalysisRepository.flush();

        try {
            // base64 인라인 대신 presigned URL 을 전달해 요청 body 를 작게 유지한다.
            // (AI 서버가 URL 로 원본 PDF 를 직접 내려받으므로 GMS 요청 크기 제한을 우회)
            String documentUrl = documentStorage
                    .createPresignedGetUrl(document.getFileKey(), ANALYSIS_URL_TTL)
                    .url();

            AnalysisResult result = analysisClient.analyze(
                    documentUrl,
                    documentType,
                    new DocumentContext(address)
            );

            documentAnalysisRepository.save(
                    DocumentAnalysis.builder()
                            .document(document)
                            .riskLevel(result.riskLevel())
                            .summary(result.summary())
                            .resultJson(result.resultJson())
                            .build()
            );
            document.markCompleted();
        } catch (RuntimeException exception) {
            log.warn("서류 분석 실패로 상태를 FAILED 처리: documentId={}, type={}",
                    documentId, documentType.getValue(), exception);
            document.markFailed();
        }
    }
}
