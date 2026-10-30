package com.backend.bangbangbwa.domain.property.dto;

import com.backend.bangbangbwa.domain.property.entity.Property;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PropertyDetailResponse {

    private Long propertyId;
    private String title;
    private String transactionType;
    private String roomType;

    /** 보증금 또는 매매가 (단위: 만원). 55000 = 5억 5천만원 */
    private Integer deposit;

    /** 월세 (단위: 만원). 매매·전세는 0 */
    private Integer monthlyRent;

    /** 관리비 (단위: 만원) */
    private Integer maintenanceFee;

    private String sigungu;
    private String dong;
    private String lotNumber;
    private String roadAddress;
    private String complexName;
    private Integer builtYear;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Double area;
    private Integer rooms;
    private Integer floor;
    private Integer totalFloor;
    private String description;
    private String status;
    private Boolean saved;
    private LocalDateTime createdAt;
    private PropertyAgentResponse agent;
    private List<PropertyImageResponse> images;
    private PropertyEnvironmentResponse environment;

    public static PropertyDetailResponse from(
            Property property,
            List<PropertyImageResponse> images,
            PropertyEnvironmentResponse environment,
            boolean saved
    ) {
        return new PropertyDetailResponse(
                property.getId(),
                property.getTitle(),
                property.getTransactionType().getValue(),
                property.getRoomType().getValue(),
                property.getDeposit(),
                property.getMonthlyRent(),
                property.getMaintenanceFee(),
                property.getSigungu(),
                property.getDong(),
                property.getLotNumber(),
                property.getRoadAddress(),
                property.getComplexName(),
                property.getBuiltYear(),
                property.getLatitude(),
                property.getLongitude(),
                property.getArea(),
                property.getRooms(),
                property.getFloor(),
                property.getTotalFloor(),
                property.getDescription(),
                property.getStatus().getValue(),
                saved,
                property.getCreatedAt(),
                PropertyAgentResponse.from(property.getRegistrant()),
                images,
                environment
        );
    }
}
