package com.backend.bangbangbwa.domain.property.entity;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum TransactionType {
    SALE("매매"),
    JEONSE("전세"),
    MONTHLY_RENT("월세");

    private final String value;

    TransactionType(String value) {
        this.value = value;
    }

    public static TransactionType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.INVALID_TRANSACTION_TYPE
                        )
                );
    }
}
