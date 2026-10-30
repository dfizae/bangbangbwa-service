package com.backend.bangbangbwa.domain.property.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class PropertyCreateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "거래 유형은 필수입니다.")
    private String transactionType;

    @NotBlank(message = "방 종류는 필수입니다.")
    private String roomType;

    /** 보증금 또는 매매가 (단위: 만원). 55000 = 5억 5천만원 */
    @NotNull(message = "보증금은 필수입니다.")
    @PositiveOrZero(message = "보증금은 0 이상이어야 합니다.")
    private Integer deposit;

    /** 월세 (단위: 만원). 매매·전세는 0 */
    @PositiveOrZero(message = "월세는 0 이상이어야 합니다.")
    private Integer monthlyRent;

    /** 관리비 (단위: 만원) */
    @PositiveOrZero(message = "관리비는 0 이상이어야 합니다.")
    private Integer maintenanceFee;

    @NotBlank(message = "시군구는 필수입니다.")
    @Size(max = 30, message = "시군구는 30자 이하여야 합니다.")
    private String sigungu;

    @NotBlank(message = "동은 필수입니다.")
    @Size(max = 30, message = "동은 30자 이하여야 합니다.")
    private String dong;

    @Size(max = 50, message = "지번은 50자 이하여야 합니다.")
    private String lotNumber;

    @Size(max = 255, message = "도로명주소는 255자 이하여야 합니다.")
    private String roadAddress;

    @Size(max = 100, message = "단지명은 100자 이하여야 합니다.")
    private String complexName;

    @Min(value = 1900, message = "건축년도는 1900 이상이어야 합니다.")
    @Max(value = 2100, message = "건축년도는 2100 이하여야 합니다.")
    private Integer builtYear;

    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
    private BigDecimal longitude;

    @Positive(message = "면적은 0보다 커야 합니다.")
    private Double area;

    private Integer floor;

    @Positive(message = "총 층수는 0보다 커야 합니다.")
    private Integer totalFloor;

    @Positive(message = "방 개수는 0보다 커야 합니다.")
    private Integer rooms;

    @Size(max = 1000, message = "상세 설명은 1000자 이하여야 합니다.")
    private String description;

    @Valid
    private PropertyEnvironmentRequest environment;
}
