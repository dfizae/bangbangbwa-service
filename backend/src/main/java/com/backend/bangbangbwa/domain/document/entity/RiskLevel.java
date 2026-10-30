package com.backend.bangbangbwa.domain.document.entity;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum RiskLevel {
    SAFE("안전"),
    CAUTION("주의"),
    DANGER("위험");

    private final String value;

    RiskLevel(String value) {
        this.value = value;
    }

    public static RiskLevel from(String value) {
        return Arrays.stream(values())
                .filter(level -> level.value.equals(value))
                .findFirst()
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.DOCUMENT_ANALYSIS_FAILED
                        )
                );
    }
}
