package com.backend.bangbangbwa.domain.property.dto;

import com.backend.bangbangbwa.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 매물을 등록한 중개사 정보 (매물 상세에 노출) */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PropertyAgentResponse {

    private Long agentId;
    private String name;
    private String phone;

    public static PropertyAgentResponse from(User registrant) {
        return new PropertyAgentResponse(
                registrant.getId(),
                registrant.getName(),
                registrant.getPhone()
        );
    }
}
