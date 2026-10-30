package com.backend.bangbangbwa.domain.property.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PropertySearchRequest {

    private String query;

    private String sigungu;

    private String transactionType;

    private String roomType;

    /** 보증금 하한 (단위: 만원). 이상 */
    private Integer minDeposit;

    /** 보증금 상한 (단위: 만원). 미만 */
    private Integer maxDeposit;
}
