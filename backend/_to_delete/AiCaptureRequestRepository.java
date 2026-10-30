package com.backend.bangbangbwa.domain.detection.repository;

import com.backend.bangbangbwa.domain.detection.entity.AiCaptureRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiCaptureRequestRepository
        extends JpaRepository<AiCaptureRequest, String> {
}
