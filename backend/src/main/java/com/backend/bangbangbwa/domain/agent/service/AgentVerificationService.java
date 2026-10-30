package com.backend.bangbangbwa.domain.agent.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.agent.dto.*;
import com.backend.bangbangbwa.domain.agent.entity.AgentVerificationRequest;
import com.backend.bangbangbwa.domain.agent.entity.AgentVerificationStatus;
import com.backend.bangbangbwa.domain.agent.repository.AgentVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AgentVerificationService {

    private static final long MAX_DOCUMENT_SIZE = 10L * 1024 * 1024;
    private static final Duration DOCUMENT_URL_DURATION = Duration.ofMinutes(10);

    private final AgentVerificationRepository verificationRepository;
    private final AgentLicenseVerificationClient licenseVerificationClient;
    private final AgentDocumentStorage documentStorage;

    /*
     * 테스트에서 시간을 고정해야 한다면 Clock을 Bean으로 등록해서 주입하는 구조로
     * 변경하는 것을 권장합니다.
     */
    private final Clock clock = Clock.systemDefaultZone();

    public LicenseCheckResponse checkLicense(String licenseNumber) {
        String validatedLicenseNumber = validateLicenseNumber(licenseNumber);
        BrokerLicenseLookupResult result =
                licenseVerificationClient.lookup(validatedLicenseNumber);

        return new LicenseCheckResponse(
                validatedLicenseNumber,
                result.valid(),
                result.brokerName(),
                result.officeName(),
                result.officeAddress(),
                result.officePhone(),
                result.businessStatus(),
                result.checkedAt()
        );
    }

    @Transactional
    public AgentVerificationResponse submit(
            Long userId,
            String licenseNumber,
            MultipartFile document
    ) {
        String validatedLicenseNumber = validateLicenseNumber(licenseNumber);
        validateDocument(document);

        AgentVerificationRequest existing =
                verificationRepository.findByUserIdForUpdate(userId)
                        .orElse(null);

        validateSubmissionState(existing);

        BrokerLicenseLookupResult lookupResult =
                licenseVerificationClient.lookup(validatedLicenseNumber);

        if (!lookupResult.valid()) {
            throw new CustomException(ErrorCode.BROKER_LICENSE_NOT_FOUND);
        }

        AgentDocumentStorage.StoredFile uploaded =
                documentStorage.upload(userId, document);

        String oldFileKey =
                existing == null ? null : existing.getDocumentFileKey();

        try {
            LocalDateTime now = LocalDateTime.now(clock);
            AgentVerificationRequest.BrokerLicenseInformation broker =
                    toBrokerInformation(lookupResult);

            AgentVerificationRequest.StoredDocument storedDocument =
                    new AgentVerificationRequest.StoredDocument(
                            uploaded.originalName(),
                            uploaded.fileKey(),
                            uploaded.contentType(),
                            uploaded.fileSize()
                    );

            AgentVerificationRequest saved;
            if (existing == null) {
                saved = verificationRepository.save(
                        AgentVerificationRequest.create(
                                userId,
                                validatedLicenseNumber,
                                broker,
                                storedDocument,
                                now
                        )
                );
            } else {
                existing.reapply(
                        validatedLicenseNumber,
                        broker,
                        storedDocument,
                        now
                );
                saved = existing;
            }

            verificationRepository.flush();

            /*
             * 재신청은 새 파일 업로드 → DB 반영 → 기존 파일 삭제 순서입니다.
             * 다만 S3 삭제는 DB 트랜잭션과 원자적이지 않으므로,
             * 운영에서는 TransactionSynchronization 또는 Outbox 적용을 권장합니다.
             */
            if (oldFileKey != null && !oldFileKey.equals(uploaded.fileKey())) {
                documentStorage.delete(oldFileKey);
            }

            return AgentVerificationResponse.from(saved);
        } catch (RuntimeException exception) {
            documentStorage.delete(uploaded.fileKey());
            throw exception;
        }
    }

    public AgentVerificationResponse getMine(Long userId) {
        AgentVerificationRequest request =
                verificationRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.BROKER_VERIFICATION_NOT_FOUND
                                )
                        );

        return AgentVerificationResponse.from(request);
    }

    public Page<AgentVerificationResponse> getAll(
            AgentVerificationStatus status,
            Pageable pageable
    ) {
        Page<AgentVerificationRequest> result =
                status == null
                        ? verificationRepository.findAll(pageable)
                        : verificationRepository.findAllByStatus(
                                status,
                                pageable
                        );

        return result.map(AgentVerificationResponse::from);
    }

    public AgentVerificationDetailResponse getDetail(Long verificationId) {
        AgentVerificationRequest request =
                verificationRepository.findById(verificationId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.BROKER_VERIFICATION_NOT_FOUND
                                )
                        );

        AgentDocumentStorage.PresignedFileUrl fileUrl =
                documentStorage.createPresignedGetUrl(
                        request.getDocumentFileKey(),
                        DOCUMENT_URL_DURATION
                );

        return AgentVerificationDetailResponse.from(
                request,
                fileUrl.url(),
                fileUrl.expiresAt()
        );
    }

    @Transactional
    public AgentVerificationResponse review(
            Long verificationId,
            AgentVerificationReviewRequest.VerificationDecision decision
    ) {
        AgentVerificationRequest request =
                verificationRepository.findByIdForUpdate(verificationId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.BROKER_VERIFICATION_NOT_FOUND
                                )
                        );

        LocalDateTime now = LocalDateTime.now(clock);

        switch (decision) {
            case APPROVED -> {
                request.approve(now);
                int updatedRows =
                        verificationRepository.promoteUserToAgent(
                                request.getUserId()
                        );

                if (updatedRows != 1) {
                    throw new CustomException(ErrorCode.USER_NOT_FOUND);
                }
            }
            case REJECTED -> request.reject(now);
            default -> throw new CustomException(
                    ErrorCode.INVALID_VERIFICATION_DECISION
            );
        }

        return AgentVerificationResponse.from(request);
    }

    private void validateSubmissionState(
            AgentVerificationRequest existing
    ) {
        if (existing == null) {
            return;
        }

        switch (existing.getStatus()) {
            case PENDING -> throw new CustomException(
                    ErrorCode.BROKER_VERIFICATION_ALREADY_PENDING
            );
            case APPROVED -> throw new CustomException(
                    ErrorCode.BROKER_VERIFICATION_ALREADY_APPROVED
            );
            case REJECTED -> {
                // 기존 행을 갱신하여 재신청합니다.
            }
        }
    }

    private AgentVerificationRequest.BrokerLicenseInformation
    toBrokerInformation(BrokerLicenseLookupResult result) {
        return new AgentVerificationRequest.BrokerLicenseInformation(
                result.externalCheckStatus(),
                result.brokerName(),
                result.officeName(),
                result.officeAddress(),
                result.officePhone(),
                result.businessStatus(),
                result.checkedAt()
        );
    }

    private String validateLicenseNumber(String licenseNumber) {
        if (licenseNumber == null || licenseNumber.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_BROKER_LICENSE_FORMAT);
        }

        String value = licenseNumber.trim();

        // 등록번호는 하이픈을 포함한 입력값 그대로 저장합니다.
        if (value.length() > 50) {
            throw new CustomException(ErrorCode.INVALID_BROKER_LICENSE_FORMAT);
        }

        return value;
    }

    private void validateDocument(MultipartFile document) {
        if (document == null || document.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_VERIFICATION_FILE);
        }

        if (document.getSize() > MAX_DOCUMENT_SIZE) {
            throw new CustomException(ErrorCode.VERIFICATION_FILE_SIZE_EXCEEDED);
        }

        String contentType = document.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_TYPE);
        }
    }

    private boolean isAllowedContentType(String contentType) {
        return contentType.equals("application/pdf")
                || contentType.equals("image/jpeg")
                || contentType.equals("image/png")
                || contentType.equals("image/webp");
    }
}
