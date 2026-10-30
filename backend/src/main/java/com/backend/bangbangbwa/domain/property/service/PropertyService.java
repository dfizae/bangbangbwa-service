package com.backend.bangbangbwa.domain.property.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.favorite.repository.FavoritePropertyRepository;
import com.backend.bangbangbwa.domain.property.dto.FilterOptionResponse;
import com.backend.bangbangbwa.domain.property.dto.PropertyCreateRequest;
import com.backend.bangbangbwa.domain.property.dto.PropertyDetailResponse;
import com.backend.bangbangbwa.domain.property.dto.PropertyMapResponse;
import com.backend.bangbangbwa.domain.property.dto.PropertySearchRequest;
import com.backend.bangbangbwa.domain.property.dto.PropertyUpdateRequest;
import com.backend.bangbangbwa.domain.property.dto.PropertyEnvironmentRequest;
import com.backend.bangbangbwa.domain.property.dto.PropertyEnvironmentResponse;
import com.backend.bangbangbwa.domain.property.dto.PropertyImageResponse;
import com.backend.bangbangbwa.domain.property.dto.PropertySummaryResponse;
import com.backend.bangbangbwa.domain.property.entity.Property;
import com.backend.bangbangbwa.domain.property.entity.PropertyEnvironment;
import com.backend.bangbangbwa.domain.property.entity.PropertyImage;
import com.backend.bangbangbwa.domain.property.entity.PropertyStatus;
import com.backend.bangbangbwa.domain.property.entity.RoomType;
import com.backend.bangbangbwa.domain.property.entity.TransactionType;
import com.backend.bangbangbwa.domain.property.event.PropertyImageDeletedEvent;
import com.backend.bangbangbwa.domain.property.event.PropertyImageUploadedEvent;
import com.backend.bangbangbwa.domain.property.repository.PropertyEnvironmentRepository;
import com.backend.bangbangbwa.domain.property.repository.PropertyImageRepository;
import com.backend.bangbangbwa.domain.property.repository.PropertyRepository;
import com.backend.bangbangbwa.domain.property.storage.StorageService;
import com.backend.bangbangbwa.domain.user.entity.User;
import com.backend.bangbangbwa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final PropertyEnvironmentRepository propertyEnvironmentRepository;
    private final FavoritePropertyRepository favoritePropertyRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int MAX_IMAGES_PER_PROPERTY = 10;
    private static final long MAX_IMAGE_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final Duration IMAGE_URL_DURATION = Duration.ofMinutes(60);

    @Transactional
    public PropertyDetailResponse createProperty(
            Long userId,
            PropertyCreateRequest request
    ) {
        User registrant = getActiveUser(userId);

        if (!registrant.isAgent()) {
            throw new CustomException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }

        TransactionType transactionType =
                TransactionType.from(request.getTransactionType());
        RoomType roomType =
                RoomType.from(request.getRoomType());

        validateFloor(request.getFloor(), request.getTotalFloor());

        Integer monthlyRent = resolveMonthlyRent(
                transactionType,
                request.getMonthlyRent()
        );

        Property property = Property.builder()
                .registrant(registrant)
                .title(request.getTitle())
                .sigungu(request.getSigungu())
                .dong(request.getDong())
                .lotNumber(request.getLotNumber())
                .roadAddress(request.getRoadAddress())
                .complexName(request.getComplexName())
                .builtYear(request.getBuiltYear())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .deposit(request.getDeposit())
                .monthlyRent(monthlyRent)
                .maintenanceFee(request.getMaintenanceFee())
                .transactionType(transactionType)
                .roomType(roomType)
                .floor(request.getFloor())
                .totalFloor(request.getTotalFloor())
                .area(request.getArea())
                .rooms(request.getRooms())
                .description(request.getDescription())
                .build();

        Property saved = propertyRepository.save(property);

        if (request.getEnvironment() != null) {
            saveEnvironment(saved, request.getEnvironment());
        }

        return toDetailResponse(saved, userId);
    }

    @Transactional
    public PropertyDetailResponse updateProperty(
            Long propertyId,
            Long userId,
            PropertyUpdateRequest request
    ) {
        Property property = propertyRepository
                .findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.PROPERTY_NOT_FOUND)
                );

        if (!property.getRegistrant().getId().equals(userId)) {
            throw new CustomException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }

        TransactionType transactionType = request.getTransactionType() != null
                ? TransactionType.from(request.getTransactionType())
                : property.getTransactionType();

        RoomType roomType = request.getRoomType() != null
                ? RoomType.from(request.getRoomType())
                : property.getRoomType();

        Integer floor = request.getFloor() != null
                ? request.getFloor()
                : property.getFloor();
        Integer totalFloor = request.getTotalFloor() != null
                ? request.getTotalFloor()
                : property.getTotalFloor();
        validateFloor(floor, totalFloor);

        Integer monthlyRent = request.getMonthlyRent() != null
                ? request.getMonthlyRent()
                : property.getMonthlyRent();
        if (transactionType == TransactionType.MONTHLY_RENT) {
            if (monthlyRent == null || monthlyRent <= 0) {
                throw new CustomException(ErrorCode.MONTHLY_RENT_REQUIRED);
            }
        } else {
            if (request.getMonthlyRent() != null && request.getMonthlyRent() != 0) {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }
            monthlyRent = 0;
        }

        boolean transactionTypeChanged =
                request.getTransactionType() != null
                        && transactionType != property.getTransactionType();
        if (transactionTypeChanged && request.getDeposit() == null) {
            throw new CustomException(ErrorCode.DEPOSIT_REQUIRED);
        }

        if ((request.getLatitude() == null)
                != (request.getLongitude() == null)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        property.update(
                request.getTitle(),
                transactionType,
                roomType,
                request.getDeposit(),
                monthlyRent,
                request.getMaintenanceFee(),
                request.getSigungu(),
                request.getDong(),
                request.getLotNumber(),
                request.getRoadAddress(),
                request.getComplexName(),
                request.getBuiltYear(),
                request.getLatitude(),
                request.getLongitude(),
                request.getArea(),
                floor,
                totalFloor,
                request.getRooms(),
                request.getDescription()
        );

        return toDetailResponse(property, userId);
    }

    @Transactional
    public void deleteProperty(Long propertyId, Long userId) {
        Property property = propertyRepository
                .findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.PROPERTY_NOT_FOUND)
                );

        if (!property.getRegistrant().getId().equals(userId)) {
            throw new CustomException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }

        property.delete();
    }

    @Transactional
    public PropertyDetailResponse changeStatus(
            Long propertyId,
            Long userId,
            String status
    ) {
        Property property = propertyRepository
                .findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.PROPERTY_NOT_FOUND)
                );

        if (!property.getRegistrant().getId().equals(userId)) {
            throw new CustomException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }

        property.changeStatus(PropertyStatus.from(status));

        return toDetailResponse(property, userId);
    }

    @Transactional
    public List<PropertyImageResponse> uploadImages(
            Long propertyId,
            Long userId,
            List<MultipartFile> files
    ) {
        Property property = propertyRepository
                .findByIdForUpdate(propertyId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.PROPERTY_NOT_FOUND)
                );

        if (!property.getRegistrant().getId().equals(userId)) {
            throw new CustomException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }

        validateImages(files);

        int existingCount = propertyImageRepository.countByPropertyId(propertyId);
        if (existingCount + files.size() > MAX_IMAGES_PER_PROPERTY) {
            throw new CustomException(ErrorCode.IMAGE_COUNT_EXCEEDED);
        }

        List<PropertyImage> saved = new ArrayList<>();
        int sequence = existingCount;
        for (MultipartFile file : files) {
            String imageUrl = storageService.upload(
                    file,
                    "property-images/" + propertyId
            );
            PropertyImage image = PropertyImage.builder()
                    .property(property)
                    .imageUrl(imageUrl)
                    .sequence(sequence++)
                    .build();
            saved.add(propertyImageRepository.save(image));
            eventPublisher.publishEvent(new PropertyImageUploadedEvent(imageUrl));
        }

        return saved.stream()
                .map(PropertyImageResponse::from)
                .toList();
    }

    @Transactional
    public void deleteImage(Long propertyId, Long imageId, Long userId) {
        Property property = propertyRepository
                .findByIdForUpdate(propertyId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.PROPERTY_NOT_FOUND)
                );

        if (!property.getRegistrant().getId().equals(userId)) {
            throw new CustomException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }

        PropertyImage image = propertyImageRepository
                .findByIdAndPropertyId(imageId, propertyId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.IMAGE_NOT_FOUND)
                );

        int deletedSequence = image.getSequence();
        String imageUrl = image.getImageUrl();

        propertyImageRepository.delete(image);
        propertyImageRepository.shiftSequenceAfterDeletion(
                propertyId,
                deletedSequence
        );
        eventPublisher.publishEvent(new PropertyImageDeletedEvent(imageUrl));
    }

    public Page<PropertySummaryResponse> getProperties(
            PropertySearchRequest request,
            Pageable pageable,
            Long userId
    ) {
        validateDepositRange(request.getMinDeposit(), request.getMaxDeposit());

        String query = StringUtils.hasText(request.getQuery())
                ? request.getQuery()
                : null;

        String sigungu = StringUtils.hasText(request.getSigungu())
                ? request.getSigungu()
                : null;

        TransactionType transactionType =
                StringUtils.hasText(request.getTransactionType())
                        ? TransactionType.from(request.getTransactionType())
                        : null;

        RoomType roomType = StringUtils.hasText(request.getRoomType())
                ? RoomType.from(request.getRoomType())
                : null;

        Page<Property> properties = propertyRepository.search(
                query,
                sigungu,
                transactionType,
                roomType,
                request.getMinDeposit(),
                request.getMaxDeposit(),
                pageable
        );

        Set<Long> favoritedIds =
                favoritedPropertyIds(userId, properties.getContent());
        Map<Long, String> thumbnails =
                thumbnailUrls(properties.getContent());
        return properties.map(property ->
                PropertySummaryResponse.from(
                        property,
                        favoritedIds.contains(property.getId()),
                        thumbnails.get(property.getId())
                ));
    }

    public PropertyDetailResponse getProperty(Long propertyId, Long userId) {
        Property property = propertyRepository
                .findByIdAndDeletedAtIsNull(propertyId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.PROPERTY_NOT_FOUND
                        )
                );

        return toDetailResponse(property, userId);
    }

    public Page<PropertySummaryResponse> getMyProperties(
            Long userId,
            Pageable pageable
    ) {
        Page<Property> properties = propertyRepository
                .findByRegistrantIdAndDeletedAtIsNull(userId, pageable);

        Set<Long> favoritedIds =
                favoritedPropertyIds(userId, properties.getContent());
        Map<Long, String> thumbnails =
                thumbnailUrls(properties.getContent());
        return properties.map(property ->
                PropertySummaryResponse.from(
                        property,
                        favoritedIds.contains(property.getId()),
                        thumbnails.get(property.getId())
                ));
    }

    /**
     * 매물 목록의 대표 이미지(첫 번째 이미지) URL을 매물 ID별로 조회합니다.
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

    public List<PropertyMapResponse> getPropertiesInBounds(
            BigDecimal swLat,
            BigDecimal swLng,
            BigDecimal neLat,
            BigDecimal neLng
    ) {
        return propertyRepository
                .findInBounds(swLat, neLat, swLng, neLng)
                .stream()
                .map(PropertyMapResponse::from)
                .toList();
    }

    public FilterOptionResponse getFilterOptions() {
        List<String> transactionTypes = Arrays.stream(TransactionType.values())
                .map(TransactionType::getValue)
                .toList();

        List<String> roomTypes = Arrays.stream(RoomType.values())
                .map(RoomType::getValue)
                .toList();

        List<String> sigungus = propertyRepository.findDistinctSigungu();

        return FilterOptionResponse.of(transactionTypes, roomTypes, sigungus);
    }

    private void saveEnvironment(
            Property property,
            PropertyEnvironmentRequest request
    ) {
        PropertyEnvironment environment = PropertyEnvironment.builder()
                .property(property)
                .nearestStationName(request.getNearestStationName())
                .stationDistanceMeter(request.getStationDistanceMeter())
                .stationWalkingMinutes(request.getStationWalkingMinutes())
                .convenienceStoreCount(request.getConvenienceStoreCount())
                .martCount(request.getMartCount())
                .hospitalCount(request.getHospitalCount())
                .pharmacyCount(request.getPharmacyCount())
                .cafeCount(request.getCafeCount())
                .policeCount(request.getPoliceCount())
                .parkCount(request.getParkCount())
                .bankCount(request.getBankCount())
                .laundryCount(request.getLaundryCount())
                .schoolCount(request.getSchoolCount())
                .build();
        propertyEnvironmentRepository.save(environment);
    }

    private PropertyDetailResponse toDetailResponse(Property property, Long userId) {
        Long propertyId = property.getId();

        List<PropertyImageResponse> images = propertyImageRepository
                .findByPropertyIdOrderBySequenceAsc(propertyId)
                .stream()
                .map(image -> PropertyImageResponse.of(
                        image,
                        storageService.createPresignedGetUrl(
                                image.getImageUrl(),
                                IMAGE_URL_DURATION
                        )
                ))
                .toList();

        PropertyEnvironmentResponse environment = propertyEnvironmentRepository
                .findByPropertyId(propertyId)
                .map(PropertyEnvironmentResponse::from)
                .orElse(null);

        boolean saved = userId != null
                && favoritePropertyRepository
                        .existsByUserIdAndPropertyId(userId, propertyId);

        return PropertyDetailResponse.from(property, images, environment, saved);
    }

    private Set<Long> favoritedPropertyIds(
            Long userId,
            List<Property> properties
    ) {
        if (userId == null || properties.isEmpty()) {
            return Set.of();
        }
        List<Long> propertyIds = properties.stream()
                .map(Property::getId)
                .toList();
        return favoritePropertyRepository
                .findFavoritedPropertyIds(userId, propertyIds);
    }

    private void validateImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new CustomException(ErrorCode.EMPTY_FILE);
            }
            if (file.getSize() > MAX_IMAGE_FILE_SIZE) {
                throw new CustomException(ErrorCode.IMAGE_SIZE_EXCEEDED);
            }
            if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(file.getContentType())) {
                throw new CustomException(ErrorCode.INVALID_IMAGE_TYPE);
            }
        }
    }

    private User getActiveUser(Long userId) {
        return userRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );
    }

    private void validateDepositRange(Integer minDeposit, Integer maxDeposit) {
        if (minDeposit == null || maxDeposit == null) {
            return;
        }

        if (minDeposit > maxDeposit) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateFloor(Integer floor, Integer totalFloor) {
        if (floor == null || totalFloor == null) {
            return;
        }

        if (floor > totalFloor) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Integer resolveMonthlyRent(
            TransactionType transactionType,
            Integer monthlyRent
    ) {
        if (transactionType == TransactionType.MONTHLY_RENT) {
            if (monthlyRent == null) {
                throw new CustomException(ErrorCode.MONTHLY_RENT_REQUIRED);
            }
            return monthlyRent;
        }

        if (monthlyRent != null && monthlyRent != 0) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        return 0;
    }
}
