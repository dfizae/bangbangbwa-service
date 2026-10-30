package com.backend.bangbangbwa.domain.document.storage;

import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 매물 서류(등기부등본·건축물대장)를 S3에 저장하는 저장소.
 *
 * 서류는 민감 정보이므로 공개 URL을 두지 않고 S3 객체 키만 보관하며,
 * 다운로드 시에만 짧은 만료 시간의 presigned URL을 발급합니다.
 */
public interface PropertyDocumentStorage {

    StoredFile upload(Long propertyId, MultipartFile file);

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
