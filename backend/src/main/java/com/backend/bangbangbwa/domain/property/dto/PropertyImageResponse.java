package com.backend.bangbangbwa.domain.property.dto;

import com.backend.bangbangbwa.domain.property.entity.PropertyImage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PropertyImageResponse {

    private Long imageId;
    private String imageUrl;
    private Integer sequence;

    public static PropertyImageResponse from(PropertyImage image) {
        return new PropertyImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getSequence()
        );
    }

    public static PropertyImageResponse of(PropertyImage image, String imageUrl) {
        return new PropertyImageResponse(
                image.getId(),
                imageUrl,
                image.getSequence()
        );
    }
}
