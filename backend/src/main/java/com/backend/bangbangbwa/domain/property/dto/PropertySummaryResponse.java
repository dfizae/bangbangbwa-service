package com.backend.bangbangbwa.domain.property.dto;

import com.backend.bangbangbwa.domain.property.entity.Property;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PropertySummaryResponse {

    private Long propertyId;
    private String title;
    private String transactionType;
    private String roomType;

    /** 보증금 또는 매매가 (단위: 만원). 55000 = 5억 5천만원 */
    private Integer deposit;

    /** 월세 (단위: 만원). 매매·전세는 0 */
    private Integer monthlyRent;

    private String sigungu;
    private String dong;
    private String complexName;
    private Double area;
    private Integer floor;
    private Integer totalFloor;
    private Integer rooms;
    private String status;
    private Boolean saved;

    /** 대표 이미지(첫 번째 이미지) URL. 이미지가 없으면 null */
    private String thumbnailUrl;

    public static PropertySummaryResponse from(Property property, boolean saved) {
        return from(property, saved, null);
    }

    public static PropertySummaryResponse from(
            Property property,
            boolean saved,
            String thumbnailUrl
    ) {
        return new PropertySummaryResponse(
                property.getId(),
                property.getTitle(),
                property.getTransactionType().getValue(),
                property.getRoomType().getValue(),
                property.getDeposit(),
                property.getMonthlyRent(),
                property.getSigungu(),
                property.getDong(),
                property.getComplexName(),
                property.getArea(),
                property.getFloor(),
                property.getTotalFloor(),
                property.getRooms(),
                property.getStatus().getValue(),
                saved,
                thumbnailUrl
        );
    }
}
