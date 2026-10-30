package com.backend.bangbangbwa.domain.agent.service;

/**
 * 공공데이터 API 연동 포트입니다.
 * 실제 공공데이터 API URL과 응답 형식에 맞는 구현체를 별도로 작성해야 합니다.
 */
public interface AgentLicenseVerificationClient {

    BrokerLicenseLookupResult lookup(String licenseNumber);
}
