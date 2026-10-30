package com.backend.bangbangbwa.domain.document.dto;

import com.backend.bangbangbwa.domain.document.entity.Document;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DocumentResponse {

    private Long documentId;
    private String documentType;
    private String status;

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getDocumentType().getValue(),
                document.getStatus().name()
        );
    }
}
