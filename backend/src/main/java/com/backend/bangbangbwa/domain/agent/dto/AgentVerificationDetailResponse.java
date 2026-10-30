package com.backend.bangbangbwa.domain.agent.dto;

import com.backend.bangbangbwa.domain.agent.entity.AgentVerificationRequest;
import com.backend.bangbangbwa.domain.agent.entity.AgentVerificationStatus;

import java.time.LocalDateTime;

public record AgentVerificationDetailResponse(
        Long verificationId,
        Long userId,
        String licenseNumber,
        String brokerName,
        String officeName,
        String officeAddress,
        String officePhone,
        String businessStatus,
        AgentVerificationStatus status,
        LocalDateTime externalCheckedAt,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        DocumentResponse document
) {
    public static AgentVerificationDetailResponse from(
            AgentVerificationRequest request,
            String presignedUrl,
            LocalDateTime urlExpiresAt
    ) {
        return new AgentVerificationDetailResponse(
                request.getVerificationId(),
                request.getUserId(),
                request.getLicenseNumber(),
                request.getBrokerName(),
                request.getOfficeName(),
                request.getOfficeAddress(),
                request.getOfficePhone(),
                request.getBusinessStatus(),
                request.getStatus(),
                request.getExternalCheckedAt(),
                request.getSubmittedAt(),
                request.getReviewedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                new DocumentResponse(
                        request.getDocumentOriginalName(),
                        request.getDocumentContentType(),
                        request.getDocumentFileSize(),
                        presignedUrl,
                        urlExpiresAt
                )
        );
    }

    public record DocumentResponse(
            String originalName,
            String contentType,
            Long fileSize,
            String fileUrl,
            LocalDateTime urlExpiresAt
    ) {
    }
}
