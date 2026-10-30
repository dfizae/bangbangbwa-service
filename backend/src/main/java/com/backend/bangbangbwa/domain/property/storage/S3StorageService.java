package com.backend.bangbangbwa.domain.property.storage;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region}")
    private String region;

    @Override
    public String upload(MultipartFile file, String directory) {
        String extension = extractExtension(file.getOriginalFilename());
        String objectKey = directory + "/" + UUID.randomUUID() + extension;

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
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public BinaryContent download(String imageUrl) {
        String objectKey = extractObjectKey(imageUrl);

        if (!StringUtils.hasText(objectKey)) {
            throw new CustomException(ErrorCode.FILE_DOWNLOAD_FAILED);
        }

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();

            ResponseBytes<GetObjectResponse> object =
                    s3Client.getObjectAsBytes(request);

            return new BinaryContent(
                    object.asByteArray(),
                    object.response().contentType()
            );
        } catch (Exception exception) {
            throw new CustomException(ErrorCode.FILE_DOWNLOAD_FAILED);
        }
    }

    @Override
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

    @Override
    public String createPresignedGetUrl(String imageUrl, Duration duration) {
        String objectKey = extractObjectKey(imageUrl);

        if (!StringUtils.hasText(objectKey)) {
            return imageUrl;
        }

        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(duration)
                        .getObjectRequest(getObjectRequest)
                        .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
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

        if (imageUrl == null || !imageUrl.startsWith(prefix)) {
            return null;
        }

        return imageUrl.substring(prefix.length());
    }
}
