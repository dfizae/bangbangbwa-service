package com.backend.bangbangbwa.domain.property.entity;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum RoomType {
    ONE_ROOM("원룸"),
    OFFICETEL("오피스텔"),
    VILLA("빌라");

    private final String value;

    RoomType(String value) {
        this.value = value;
    }

    public static RoomType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.INVALID_ROOM_TYPE
                        )
                );
    }
}
