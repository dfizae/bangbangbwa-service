package com.backend.bangbangbwa.domain.oauth.service;

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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ImageService {

    private static final long MAX_FILE_SIZE =
            5L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region}")
    private String region;

    public String uploadProfileImage(
            Long userId,
            MultipartFile file
    ) {
        validateImage(file);

        String extension =
                extractExtension(
                        file.getOriginalFilename()
                );

        String objectKey =
                "profile-images/"
                        + userId
                        + "/"
                        + UUID.randomUUID()
                        + extension;

        try {
            PutObjectRequest putObjectRequest =
                    PutObjectRequest.builder()
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

            return createFileUrl(objectKey);

        } catch (IOException exception) {
            throw new CustomException(
                    ErrorCode.FILE_UPLOAD_FAILED
            );
        }
    }

    public void delete(String imageUrl) {
        String objectKey = extractObjectKey(imageUrl);

        if (!StringUtils.hasText(objectKey)) {
            return;
        }

        DeleteObjectRequest request =
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .build();

        s3Client.deleteObject(request);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(
                    ErrorCode.EMPTY_FILE
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(
                    ErrorCode.FILE_SIZE_EXCEEDED
            );
        }

        if (!ALLOWED_CONTENT_TYPES.contains(
                file.getContentType()
        )) {
            throw new CustomException(
                    ErrorCode.INVALID_IMAGE_TYPE
            );
        }
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }

        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex < 0) {
            return "";
        }

        return filename
                .substring(dotIndex)
                .toLowerCase();
    }

    private String createFileUrl(String objectKey) {
        return "https://"
                + bucket
                + ".s3."
                + region
                + ".amazonaws.com/"
                + objectKey;
    }

    private String extractObjectKey(String imageUrl) {
        String prefix =
                "https://"
                        + bucket
                        + ".s3."
                        + region
                        + ".amazonaws.com/";

        if (imageUrl == null
                || !imageUrl.startsWith(prefix)) {
            return null;
        }

        return imageUrl.substring(prefix.length());
    }
}