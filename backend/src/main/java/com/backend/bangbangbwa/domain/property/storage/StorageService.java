package com.backend.bangbangbwa.domain.property.storage;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.time.Duration;

public interface StorageService {

    String upload(MultipartFile file, String directory);

    void delete(String imageUrl);

    String createPresignedGetUrl(String imageUrl, Duration duration);

    /**
     * 스토리지에 저장된 이미지를 바이트로 내려받습니다.
     * 버킷을 private으로 유지한 채 AI 전송·PDF 임베딩에 필요한 원본 바이트를 얻기 위해 사용합니다.
     */
    BinaryContent download(String imageUrl);

    /**
     * 다운로드한 바이너리와 콘텐츠 타입을 담는 값 객체.
     */
    record BinaryContent(byte[] bytes, String contentType) {

        private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

        /**
         * {@code data:<type>;base64,<data>} 형식의 data URI로 변환합니다.
         * OpenAI 이미지 입력과 PDF {@code <img>} 임베딩에서 공통으로 사용됩니다.
         */
        public String toDataUri() {
            String type = StringUtils.hasText(contentType)
                    ? contentType
                    : DEFAULT_CONTENT_TYPE;

            return "data:" + type + ";base64,"
                    + Base64.getEncoder().encodeToString(bytes);
        }
    }
}
