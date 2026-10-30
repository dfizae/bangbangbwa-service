package com.backend.bangbangbwa.domain.favorite.repository;

import com.backend.bangbangbwa.domain.favorite.entity.FavoriteProperty;
import com.backend.bangbangbwa.domain.property.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface FavoritePropertyRepository
        extends JpaRepository<FavoriteProperty, Long> {

    boolean existsByUserIdAndPropertyId(Long userId, Long propertyId);

    long deleteByUserIdAndPropertyId(Long userId, Long propertyId);

    @Modifying
    @Query(value = """
            INSERT INTO favorite_property (user_id, property_id, created_at)
            VALUES (:userId, :propertyId, now())
            ON CONFLICT (user_id, property_id) DO NOTHING
            """, nativeQuery = true)
    void insertIgnoreConflict(
            @Param("userId") Long userId,
            @Param("propertyId") Long propertyId
    );

    @Query(value = """
            SELECT f.property FROM FavoriteProperty f
            WHERE f.user.id = :userId
              AND f.property.deletedAt IS NULL
            ORDER BY f.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(f) FROM FavoriteProperty f
            WHERE f.user.id = :userId
              AND f.property.deletedAt IS NULL
            """)
    Page<Property> findFavoritedProperties(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            SELECT f.property.id FROM FavoriteProperty f
            WHERE f.user.id = :userId
              AND f.property.id IN :propertyIds
            """)
    Set<Long> findFavoritedPropertyIds(
            @Param("userId") Long userId,
            @Param("propertyIds") List<Long> propertyIds
    );
}
