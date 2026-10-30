package com.backend.bangbangbwa.domain.property.entity;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum PropertyStatus {
    AVAILABLE("거래가능"),
    RESERVED("예약중"),
    COMPLETED("거래완료");

    private final String value;

    PropertyStatus(String value) {
        this.value = value;
    }

    public static PropertyStatus from(String value) {
        return Arrays.stream(values())
                .filter(status -> status.value.equals(value))
                .findFirst()
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.INVALID_PROPERTY_STATUS
                        )
                );
    }
}
