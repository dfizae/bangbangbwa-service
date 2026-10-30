package com.backend.bangbangbwa.domain.document.entity;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum DocumentType {
    REGISTER("등기부등본"),
    BUILDING_LEDGER("건축물대장");

    private final String value;

    DocumentType(String value) {
        this.value = value;
    }

    public static DocumentType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.INVALID_DOCUMENT_CATEGORY
                        )
                );
    }
}
