package com.backend.bangbangbwa.domain.property.event;

import com.backend.bangbangbwa.domain.property.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PropertyImageStorageEventListener {

    private final StorageService storageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleImageDeleted(PropertyImageDeletedEvent event) {
        storageService.delete(event.imageUrl());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleUploadRolledBack(PropertyImageUploadedEvent event) {
        storageService.delete(event.imageUrl());
    }
}
