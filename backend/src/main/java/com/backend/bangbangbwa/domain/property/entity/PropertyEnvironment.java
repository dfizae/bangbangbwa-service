package com.backend.bangbangbwa.domain.property.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "property_environment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyEnvironment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "environment_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "nearest_station_name", length = 100)
    private String nearestStationName;

    @Column(name = "station_distance_meter")
    private Integer stationDistanceMeter;

    @Column(name = "station_walking_minutes")
    private Integer stationWalkingMinutes;

    @Column(name = "convenience_store_count")
    private Integer convenienceStoreCount;

    @Column(name = "mart_count")
    private Integer martCount;

    @Column(name = "hospital_count")
    private Integer hospitalCount;

    @Column(name = "pharmacy_count")
    private Integer pharmacyCount;

    @Column(name = "cafe_count")
    private Integer cafeCount;

    @Column(name = "police_count")
    private Integer policeCount;

    @Column(name = "park_count")
    private Integer parkCount;

    @Column(name = "bank_count")
    private Integer bankCount;

    @Column(name = "laundry_count")
    private Integer laundryCount;

    @Column(name = "school_count")
    private Integer schoolCount;

    @Builder
    private PropertyEnvironment(
            Property property,
            String nearestStationName,
            Integer stationDistanceMeter,
            Integer stationWalkingMinutes,
            Integer convenienceStoreCount,
            Integer martCount,
            Integer hospitalCount,
            Integer pharmacyCount,
            Integer cafeCount,
            Integer policeCount,
            Integer parkCount,
            Integer bankCount,
            Integer laundryCount,
            Integer schoolCount
    ) {
        this.property = property;
        this.nearestStationName = nearestStationName;
        this.stationDistanceMeter = stationDistanceMeter;
        this.stationWalkingMinutes = stationWalkingMinutes;
        this.convenienceStoreCount = convenienceStoreCount;
        this.martCount = martCount;
        this.hospitalCount = hospitalCount;
        this.pharmacyCount = pharmacyCount;
        this.cafeCount = cafeCount;
        this.policeCount = policeCount;
        this.parkCount = parkCount;
        this.bankCount = bankCount;
        this.laundryCount = laundryCount;
        this.schoolCount = schoolCount;
    }
}
