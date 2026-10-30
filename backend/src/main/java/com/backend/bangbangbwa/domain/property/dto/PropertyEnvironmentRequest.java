package com.backend.bangbangbwa.domain.property.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PropertyEnvironmentRequest {

    @Size(max = 100, message = "역명은 100자 이하여야 합니다.")
    private String nearestStationName;

    @PositiveOrZero(message = "역 거리는 0 이상이어야 합니다.")
    private Integer stationDistanceMeter;

    @PositiveOrZero(message = "도보 시간은 0 이상이어야 합니다.")
    private Integer stationWalkingMinutes;

    @PositiveOrZero(message = "편의점 수는 0 이상이어야 합니다.")
    private Integer convenienceStoreCount;

    @PositiveOrZero(message = "마트 수는 0 이상이어야 합니다.")
    private Integer martCount;

    @PositiveOrZero(message = "병원 수는 0 이상이어야 합니다.")
    private Integer hospitalCount;

    @PositiveOrZero(message = "약국 수는 0 이상이어야 합니다.")
    private Integer pharmacyCount;

    @PositiveOrZero(message = "카페 수는 0 이상이어야 합니다.")
    private Integer cafeCount;

    @PositiveOrZero(message = "경찰서 수는 0 이상이어야 합니다.")
    private Integer policeCount;

    @PositiveOrZero(message = "공원 수는 0 이상이어야 합니다.")
    private Integer parkCount;

    @PositiveOrZero(message = "은행 수는 0 이상이어야 합니다.")
    private Integer bankCount;

    @PositiveOrZero(message = "빨래방 수는 0 이상이어야 합니다.")
    private Integer laundryCount;

    @PositiveOrZero(message = "학교 수는 0 이상이어야 합니다.")
    private Integer schoolCount;
}
