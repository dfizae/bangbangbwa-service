package com.backend.bangbangbwa.domain.agent.service;

import java.time.LocalDateTime;

public record BrokerLicenseLookupResult(
        boolean valid,
        String externalCheckStatus,
        String brokerName,
        String officeName,
        String officeAddress,
        String officePhone,
        String businessStatus,
        LocalDateTime checkedAt
) {
}
