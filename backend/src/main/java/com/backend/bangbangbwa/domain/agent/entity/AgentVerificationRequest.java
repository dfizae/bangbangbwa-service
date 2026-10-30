package com.backend.bangbangbwa.domain.agent.entity;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "agent_verification_request",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_agent_verification_user",
                        columnNames = "user_id"
                ),
                @UniqueConstraint(
                        name = "uq_agent_verification_document_file_key",
                        columnNames = "document_file_key"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentVerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long verificationId;

    /*
     * users.user_id를 참조합니다.
     * User 엔티티 패키지/필드명에 의존하지 않도록 Long FK로 매핑했습니다.
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "license_number", nullable = false, length = 50)
    private String licenseNumber;

    @Column(name = "external_check_status", nullable = false, length = 20)
    private String externalCheckStatus;

    @Column(name = "broker_name", length = 100)
    private String brokerName;

    @Column(name = "office_name", length = 150)
    private String officeName;

    @Column(name = "office_address", length = 500)
    private String officeAddress;

    @Column(name = "office_phone", length = 30)
    private String officePhone;

    @Column(name = "business_status", length = 50)
    private String businessStatus;

    @Column(name = "external_checked_at", nullable = false)
    private LocalDateTime externalCheckedAt;

    @Column(name = "document_original_name", nullable = false, length = 255)
    private String documentOriginalName;

    @Column(name = "document_file_key", nullable = false, length = 500)
    private String documentFileKey;

    @Column(name = "document_content_type", nullable = false, length = 100)
    private String documentContentType;

    @Column(name = "document_file_size", nullable = false)
    private Long documentFileSize;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgentVerificationStatus status;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AgentVerificationRequest create(
            Long userId,
            String licenseNumber,
            BrokerLicenseInformation broker,
            StoredDocument document,
            LocalDateTime now
    ) {
        AgentVerificationRequest request = new AgentVerificationRequest();
        request.userId = userId;
        request.applySubmission(licenseNumber, broker, document, now);
        request.createdAt = now;
        return request;
    }

    public void reapply(
            String licenseNumber,
            BrokerLicenseInformation broker,
            StoredDocument document,
            LocalDateTime now
    ) {
        if (status != AgentVerificationStatus.REJECTED) {
            throw new CustomException(ErrorCode.INVALID_AGENT_VERIFICATION_STATUS);
        }

        applySubmission(licenseNumber, broker, document, now);
    }

    private void applySubmission(
            String licenseNumber,
            BrokerLicenseInformation broker,
            StoredDocument document,
            LocalDateTime now
    ) {
        this.licenseNumber = licenseNumber;
        this.externalCheckStatus = broker.externalCheckStatus();
        this.brokerName = broker.brokerName();
        this.officeName = broker.officeName();
        this.officeAddress = broker.officeAddress();
        this.officePhone = broker.officePhone();
        this.businessStatus = broker.businessStatus();
        this.externalCheckedAt = broker.checkedAt();

        this.documentOriginalName = document.originalName();
        this.documentFileKey = document.fileKey();
        this.documentContentType = document.contentType();
        this.documentFileSize = document.fileSize();

        this.submittedAt = now;
        this.status = AgentVerificationStatus.PENDING;
        this.reviewedAt = null;
        this.updatedAt = now;
    }

    public void approve(LocalDateTime now) {
        validatePending();
        this.status = AgentVerificationStatus.APPROVED;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    public void reject(LocalDateTime now) {
        validatePending();
        this.status = AgentVerificationStatus.REJECTED;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    private void validatePending() {
        if (status != AgentVerificationStatus.PENDING) {
            throw new CustomException(ErrorCode.AGENT_VERIFICATION_ALREADY_REVIEWED);
        }
    }

    public record BrokerLicenseInformation(
            String externalCheckStatus,
            String brokerName,
            String officeName,
            String officeAddress,
            String officePhone,
            String businessStatus,
            LocalDateTime checkedAt
    ) {
    }

    public record StoredDocument(
            String originalName,
            String fileKey,
            String contentType,
            Long fileSize
    ) {
    }
}
