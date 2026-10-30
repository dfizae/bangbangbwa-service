package com.backend.bangbangbwa.domain.checklist.dto;

import com.backend.bangbangbwa.domain.checklist.entity.Checklist;
import com.backend.bangbangbwa.domain.checklist.entity.ChecklistItem;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChecklistResponse {

    private Long checklistId;
    private Long meetingId;
    private String title;
    private LocalDateTime createdAt;
    private List<ChecklistItemResponse> items;

    public static ChecklistResponse from(
            Checklist checklist,
            List<ChecklistItem> items
    ) {
        List<ChecklistItemResponse> itemResponses = items.stream()
                .map(ChecklistItemResponse::from)
                .toList();

        return new ChecklistResponse(
                checklist.getId(),
                checklist.getMeeting().getId(),
                checklist.getTitle(),
                checklist.getCreatedAt(),
                itemResponses
        );
    }
}