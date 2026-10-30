package com.backend.bangbangbwa.domain.property.dto;

import com.backend.bangbangbwa.domain.property.entity.PropertyEnvironment;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PropertyEnvironmentResponse {

    private String nearestStationName;
    private Integer stationDistanceMeter;
    private Integer stationWalkingMinutes;
    private Integer convenienceStoreCount;
    private Integer martCount;
    private Integer hospitalCount;
    private Integer pharmacyCount;
    private Integer cafeCount;
    private Integer policeCount;
    private Integer parkCount;
    private Integer bankCount;
    private Integer laundryCount;
    private Integer schoolCount;

    public static PropertyEnvironmentResponse from(PropertyEnvironment environment) {
        return new PropertyEnvironmentResponse(
                environment.getNearestStationName(),
                environment.getStationDistanceMeter(),
                environment.getStationWalkingMinutes(),
                environment.getConvenienceStoreCount(),
                environment.getMartCount(),
                environment.getHospitalCount(),
                environment.getPharmacyCount(),
                environment.getCafeCount(),
                environment.getPoliceCount(),
                environment.getParkCount(),
                environment.getBankCount(),
                environment.getLaundryCount(),
                environment.getSchoolCount()
        );
    }
}
