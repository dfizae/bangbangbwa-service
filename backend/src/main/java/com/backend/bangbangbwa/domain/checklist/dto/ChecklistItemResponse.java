package com.backend.bangbangbwa.domain.checklist.dto;

import com.backend.bangbangbwa.domain.checklist.entity.ChecklistItem;
import com.backend.bangbangbwa.domain.checklist.entity.ChecklistItemStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChecklistItemResponse {

    private Long itemId;
    private String content;
    private ChecklistItemStatus status;
    private String memo;

    public static ChecklistItemResponse from(ChecklistItem item) {
        return new ChecklistItemResponse(
                item.getId(),
                item.getContent(),
                item.getStatus(),
                item.getMemo()
        );
    }
}