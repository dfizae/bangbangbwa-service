package com.backend.bangbangbwa.domain.property.dto;

import com.backend.bangbangbwa.domain.property.entity.Property;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PropertyMapResponse {

    private Long propertyId;
    private String title;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String transactionType;

    /** 보증금 또는 매매가 (단위: 만원). 55000 = 5억 5천만원 */
    private Integer deposit;

    /** 월세 (단위: 만원). 매매·전세는 0 */
    private Integer monthlyRent;

    public static PropertyMapResponse from(Property property) {
        return new PropertyMapResponse(
                property.getId(),
                property.getTitle(),
                property.getLatitude(),
                property.getLongitude(),
                property.getTransactionType().getValue(),
                property.getDeposit(),
                property.getMonthlyRent()
        );
    }
}
