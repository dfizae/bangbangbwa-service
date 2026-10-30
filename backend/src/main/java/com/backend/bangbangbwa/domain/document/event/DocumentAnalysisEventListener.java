package com.backend.bangbangbwa.domain.document.event;

import com.backend.bangbangbwa.domain.document.service.DocumentAnalysisProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DocumentAnalysisEventListener {

    private final DocumentAnalysisProcessor processor;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        processor.process(
                event.documentId(),
                event.documentType(),
                event.address()
        );
    }
}
