package com.backend.bangbangbwa.domain.property.repository;

import com.backend.bangbangbwa.domain.property.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

    List<PropertyImage> findByPropertyIdOrderBySequenceAsc(Long propertyId);

    @Query("""
            SELECT pi
            FROM PropertyImage pi
            WHERE pi.property.id IN :propertyIds
            ORDER BY pi.property.id ASC, pi.sequence ASC
            """)
    List<PropertyImage> findByPropertyIdsOrderBySequence(
            @Param("propertyIds") Collection<Long> propertyIds
    );

    Optional<PropertyImage> findByIdAndPropertyId(Long id, Long propertyId);

    /**
     * 여러 매물의 첫 번째 이미지(sequence 최소)를 한 번에 조회합니다.
     * 목록 조회의 N+1을 방지하기 위한 용도입니다.
     * 반환 행: [property_id, image_url]
     */
    @Query(value = """
            SELECT DISTINCT ON (property_id) property_id, image_url
              FROM property_image
             WHERE property_id IN (:propertyIds)
             ORDER BY property_id, sequence ASC
            """, nativeQuery = true)
    List<Object[]> findFirstImagesByPropertyIds(
            @Param("propertyIds") List<Long> propertyIds
    );

    int countByPropertyId(Long propertyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PropertyImage pi
            SET pi.sequence = pi.sequence - 1
            WHERE pi.property.id = :propertyId
              AND pi.sequence > :sequence
            """)
    void shiftSequenceAfterDeletion(
            @Param("propertyId") Long propertyId,
            @Param("sequence") int sequence
    );
}
