package com.backend.bangbangbwa.domain.agent.dto;

import java.time.LocalDateTime;

public record LicenseCheckResponse(
        String licenseNumber,
        boolean valid,
        String brokerName,
        String officeName,
        String officeAddress,
        String officePhone,
        String businessStatus,
        LocalDateTime checkedAt
) {
}
