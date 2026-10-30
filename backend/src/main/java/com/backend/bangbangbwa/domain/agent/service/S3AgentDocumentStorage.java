package com.backend.bangbangbwa.domain.agent.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 중개사 인증 서류를 S3에 저장하는 구현체입니다.
 *
 * - 서류는 민감 정보이므로 공개 URL을 두지 않고, 저장 시 S3 객체 키만 보관합니다.
 * - 관리자 상세 조회 시에만 짧은 만료 시간의 presigned URL을 발급합니다.
 * - 파일 검증(크기/타입/빈 파일)은 AgentVerificationService에서 이미 수행하므로
 *   여기서는 저장/삭제/URL 발급만 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Profile("!local")
public class S3AgentDocumentStorage implements AgentDocumentStorage {

    private static final String KEY_PREFIX = "agent-verifications";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public StoredFile upload(Long userId, MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        String objectKey = KEY_PREFIX
                + "/" + userId
                + "/" + UUID.randomUUID()
                + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(
                            file.getInputStream(),
                            file.getSize()
                    )
            );
        } catch (IOException exception) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return new StoredFile(
                file.getOriginalFilename(),
                objectKey,
                file.getContentType(),
                file.getSize()
        );
    }

    @Override
    public void delete(String fileKey) {
        if (!StringUtils.hasText(fileKey)) {
            return;
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .build();

        s3Client.deleteObject(request);
    }

    @Override
    public PresignedFileUrl createPresignedGetUrl(
            String fileKey,
            Duration duration
    ) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(duration)
                        .getObjectRequest(getObjectRequest)
                        .build();

        PresignedGetObjectRequest presigned =
                s3Presigner.presignGetObject(presignRequest);

        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                presigned.expiration(),
                ZoneId.systemDefault()
        );

        return new PresignedFileUrl(presigned.url().toString(), expiresAt);
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }

        return filename.substring(dotIndex).toLowerCase();
    }
}
