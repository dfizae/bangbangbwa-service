package com.backend.bangbangbwa.domain.property.repository;

import com.backend.bangbangbwa.domain.property.entity.Property;
import com.backend.bangbangbwa.domain.property.entity.RoomType;
import com.backend.bangbangbwa.domain.property.entity.TransactionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    Optional<Property> findByIdAndDeletedAtIsNull(Long propertyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM Property p
            WHERE p.id = :id
              AND p.deletedAt IS NULL
            """)
    Optional<Property> findByIdForUpdate(@Param("id") Long id);

    Page<Property> findByRegistrantIdAndDeletedAtIsNull(Long registrantId, Pageable pageable);

    @Query("""
            SELECT p FROM Property p
            WHERE p.deletedAt IS NULL
              AND p.status <> com.backend.bangbangbwa.domain.property.entity.PropertyStatus.COMPLETED
              AND (:query IS NULL OR p.title LIKE %:query%
                                  OR p.sigungu LIKE %:query%
                                  OR p.dong LIKE %:query%
                                  OR EXISTS (SELECT e FROM PropertyEnvironment e
                                             WHERE e.property = p
                                               AND e.nearestStationName LIKE %:query%))
              AND (:sigungu IS NULL OR p.sigungu = :sigungu)
              AND (:transactionType IS NULL OR p.transactionType = :transactionType)
              AND (:roomType IS NULL OR p.roomType = :roomType)
              AND (:minDeposit IS NULL OR p.deposit >= :minDeposit)
              AND (:maxDeposit IS NULL OR p.deposit < :maxDeposit)
            """)
    Page<Property> search(
            @Param("query") String query,
            @Param("sigungu") String sigungu,
            @Param("transactionType") TransactionType transactionType,
            @Param("roomType") RoomType roomType,
            @Param("minDeposit") Integer minDeposit,
            @Param("maxDeposit") Integer maxDeposit,
            Pageable pageable
    );

    @Query("""
            SELECT p FROM Property p
            WHERE p.deletedAt IS NULL
              AND p.status <> com.backend.bangbangbwa.domain.property.entity.PropertyStatus.COMPLETED
              AND p.latitude BETWEEN :swLat AND :neLat
              AND p.longitude BETWEEN :swLng AND :neLng
            """)
    List<Property> findInBounds(
            @Param("swLat") BigDecimal swLat,
            @Param("neLat") BigDecimal neLat,
            @Param("swLng") BigDecimal swLng,
            @Param("neLng") BigDecimal neLng
    );

    @Query("""
            SELECT DISTINCT p.sigungu FROM Property p
            WHERE p.deletedAt IS NULL
              AND p.sigungu IS NOT NULL
              AND TRIM(p.sigungu) <> ''
            ORDER BY p.sigungu
            """)
    List<String> findDistinctSigungu();
}
