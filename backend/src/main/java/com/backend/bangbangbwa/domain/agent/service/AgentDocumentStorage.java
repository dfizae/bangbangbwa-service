package com.backend.bangbangbwa.domain.agent.service;

import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;

public interface AgentDocumentStorage {

    StoredFile upload(Long userId, MultipartFile file);

    void delete(String fileKey);

    PresignedFileUrl createPresignedGetUrl(String fileKey, Duration duration);

    record StoredFile(
            String originalName,
            String fileKey,
            String contentType,
            long fileSize
    ) {
    }

    record PresignedFileUrl(
            String url,
            LocalDateTime expiresAt
    ) {
    }
}
