package com.backend.bangbangbwa.domain.document.storage;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

@Service
@RequiredArgsConstructor
public class S3PropertyDocumentStorage implements PropertyDocumentStorage {

    private static final String KEY_PREFIX = "property-documents";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public StoredFile upload(Long propertyId, MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        String objectKey = KEY_PREFIX
                + "/" + propertyId
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
