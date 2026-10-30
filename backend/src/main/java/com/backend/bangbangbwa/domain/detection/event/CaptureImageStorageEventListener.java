package com.backend.bangbangbwa.domain.detection.event;

import com.backend.bangbangbwa.domain.property.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CaptureImageStorageEventListener {

    private final StorageService storageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleUploadRolledBack(
            CaptureImageUploadedEvent event
    ) {
        storageService.delete(event.imageUrl());
    }
}
