package com.backend.bangbangbwa.domain.property.entity;

import com.backend.bangbangbwa.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "property")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrant_id", nullable = false)
    private User registrant;

    @Column(length = 100)
    private String title;

    @Column(length = 30)
    private String sigungu;

    @Column(length = 30)
    private String dong;

    @Column(name = "lot_number", length = 50)
    private String lotNumber;

    @Column(name = "road_address", length = 255)
    private String roadAddress;

    @Column(name = "complex_name", length = 100)
    private String complexName;

    @Column(name = "built_year")
    private Integer builtYear;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    /** 보증금 또는 매매가 (단위: 만원). 55000 = 5억 5천만원 */
    private Integer deposit;

    /** 월세 (단위: 만원). 매매·전세는 0 */
    @Column(name = "monthly_rent")
    private Integer monthlyRent;

    /** 관리비 (단위: 만원) */
    @Column(name = "maintenance_fee")
    private Integer maintenanceFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 30)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", length = 30)
    private RoomType roomType;

    private Integer floor;

    @Column(name = "total_floor")
    private Integer totalFloor;

    private Double area;

    @Column(name = "rooms")
    private Integer rooms;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PropertyStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private Property(
            User registrant,
            String title,
            String sigungu,
            String dong,
            String lotNumber,
            String roadAddress,
            String complexName,
            Integer builtYear,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer deposit,
            Integer monthlyRent,
            Integer maintenanceFee,
            TransactionType transactionType,
            RoomType roomType,
            Integer floor,
            Integer totalFloor,
            Double area,
            Integer rooms,
            String description
    ) {
        this.registrant = registrant;
        this.title = title;
        this.sigungu = sigungu;
        this.dong = dong;
        this.lotNumber = lotNumber;
        this.roadAddress = roadAddress;
        this.complexName = complexName;
        this.builtYear = builtYear;
        this.latitude = latitude;
        this.longitude = longitude;
        this.deposit = deposit;
        this.monthlyRent = monthlyRent;
        this.maintenanceFee = maintenanceFee;
        this.transactionType = transactionType;
        this.roomType = roomType;
        this.floor = floor;
        this.totalFloor = totalFloor;
        this.area = area;
        this.rooms = rooms;
        this.description = description;
        this.status = PropertyStatus.AVAILABLE;
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(
            String title,
            TransactionType transactionType,
            RoomType roomType,
            Integer deposit,
            Integer monthlyRent,
            Integer maintenanceFee,
            String sigungu,
            String dong,
            String lotNumber,
            String roadAddress,
            String complexName,
            Integer builtYear,
            BigDecimal latitude,
            BigDecimal longitude,
            Double area,
            Integer floor,
            Integer totalFloor,
            Integer rooms,
            String description
    ) {
        if (StringUtils.hasText(title)) {
            this.title = title;
        }
        if (transactionType != null) {
            this.transactionType = transactionType;
        }
        if (roomType != null) {
            this.roomType = roomType;
        }
        if (deposit != null) {
            this.deposit = deposit;
        }
        if (monthlyRent != null) {
            this.monthlyRent = monthlyRent;
        }
        if (maintenanceFee != null) {
            this.maintenanceFee = maintenanceFee;
        }
        if (StringUtils.hasText(sigungu)) {
            this.sigungu = sigungu;
        }
        if (StringUtils.hasText(dong)) {
            this.dong = dong;
        }
        if (lotNumber != null) {
            this.lotNumber = lotNumber;
        }
        if (roadAddress != null) {
            this.roadAddress = roadAddress;
        }
        if (complexName != null) {
            this.complexName = complexName;
        }
        if (builtYear != null) {
            this.builtYear = builtYear;
        }
        if (latitude != null) {
            this.latitude = latitude;
        }
        if (longitude != null) {
            this.longitude = longitude;
        }
        if (area != null) {
            this.area = area;
        }
        if (floor != null) {
            this.floor = floor;
        }
        if (totalFloor != null) {
            this.totalFloor = totalFloor;
        }
        if (rooms != null) {
            this.rooms = rooms;
        }
        if (description != null) {
            this.description = description;
        }
    }

    public void changeStatus(PropertyStatus status) {
        this.status = status;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
