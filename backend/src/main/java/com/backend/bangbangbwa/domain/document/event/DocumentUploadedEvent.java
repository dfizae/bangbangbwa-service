package com.backend.bangbangbwa.domain.document.event;

import com.backend.bangbangbwa.domain.document.entity.DocumentType;

public record DocumentUploadedEvent(
        Long documentId,
        DocumentType documentType,
        String address
) {
}
