package com.backend.bangbangbwa.domain.favorite.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.favorite.repository.FavoritePropertyRepository;
import com.backend.bangbangbwa.domain.property.dto.PropertySummaryResponse;
import com.backend.bangbangbwa.domain.property.entity.Property;
import com.backend.bangbangbwa.domain.property.repository.PropertyImageRepository;
import com.backend.bangbangbwa.domain.property.repository.PropertyRepository;
import com.backend.bangbangbwa.domain.property.storage.StorageService;
import com.backend.bangbangbwa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private static final Duration IMAGE_URL_DURATION = Duration.ofMinutes(60);

    private final FavoritePropertyRepository favoritePropertyRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    @Transactional
    public void addFavorite(Long userId, Long propertyId) {
        validateActiveUser(userId);
        validateActiveProperty(propertyId);

        favoritePropertyRepository.insertIgnoreConflict(userId, propertyId);
    }

    @Transactional
    public void removeFavorite(Long userId, Long propertyId) {
        favoritePropertyRepository
                .deleteByUserIdAndPropertyId(userId, propertyId);
    }

    public Page<PropertySummaryResponse> getMyFavorites(
            Long userId,
            Pageable pageable
    ) {
        validateActiveUser(userId);

        Page<Property> favorites = favoritePropertyRepository
                .findFavoritedProperties(userId, pageable);
        Map<Long, String> thumbnails = thumbnailUrls(favorites.getContent());

        return favorites.map(property -> PropertySummaryResponse.from(
                property,
                true,
                thumbnails.get(property.getId())
        ));
    }

    /**
     * 관심 매물 목록의 대표 이미지(첫 번째 이미지) URL을 매물 ID별로 조회합니다.
     * 이미지 조회 N+1을 피하기 위해 한 번의 쿼리로 가져오고,
     * 비공개 버킷 대응을 위해 presigned URL로 변환합니다.
     */
    private Map<Long, String> thumbnailUrls(List<Property> properties) {
        if (properties.isEmpty()) {
            return Map.of();
        }

        List<Long> propertyIds = properties.stream()
                .map(Property::getId)
                .toList();

        Map<Long, String> thumbnails = new HashMap<>();
        for (Object[] row :
                propertyImageRepository.findFirstImagesByPropertyIds(propertyIds)) {
            Long propertyId = ((Number) row[0]).longValue();
            String imageUrl = (String) row[1];
            thumbnails.put(
                    propertyId,
                    storageService.createPresignedGetUrl(
                            imageUrl,
                            IMAGE_URL_DURATION
                    )
            );
        }
        return thumbnails;
    }

    private void validateActiveUser(Long userId) {
        if (userRepository.findByIdAndDeletedAtIsNull(userId).isEmpty()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateActiveProperty(Long propertyId) {
        if (propertyRepository.findByIdAndDeletedAtIsNull(propertyId).isEmpty()) {
            throw new CustomException(ErrorCode.PROPERTY_NOT_FOUND);
        }
    }
}
