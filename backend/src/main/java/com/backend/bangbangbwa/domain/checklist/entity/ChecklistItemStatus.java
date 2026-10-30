package com.backend.bangbangbwa.domain.checklist.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ChecklistItemStatus {

    PENDING, // 확인 전
    COMPLETED, // 확인 완료
    ISSUE_FOUND; //문제 발견

    //이상값 들어오면 null로 처리.
    @JsonCreator
    public static ChecklistItemStatus from(String value) {
        if (value == null) {
            return null;
        }

        try {
            return ChecklistItemStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
