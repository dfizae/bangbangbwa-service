package com.backend.bangbangbwa.domain.document.dto;

import com.backend.bangbangbwa.domain.document.entity.Document;
import com.backend.bangbangbwa.domain.document.entity.DocumentAnalysis;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DocumentSummaryResponse {

    private Long documentId;
    private String documentType;
    private String status;
    private String riskLevel;
    private String summary;
    private Boolean downloadable;

    public static DocumentSummaryResponse from(
            Document document,
            DocumentAnalysis analysis
    ) {
        String riskLevel = analysis != null
                ? analysis.getRiskLevel().getValue()
                : null;
        String summary = analysis != null ? analysis.getSummary() : null;

        return new DocumentSummaryResponse(
                document.getId(),
                document.getDocumentType().getValue(),
                document.getStatus().name(),
                riskLevel,
                summary,
                document.isCompleted()
        );
    }
}
