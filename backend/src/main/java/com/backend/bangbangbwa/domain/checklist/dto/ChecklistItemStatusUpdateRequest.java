package com.backend.bangbangbwa.domain.checklist.dto;

import com.backend.bangbangbwa.domain.checklist.entity.ChecklistItemStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChecklistItemStatusUpdateRequest {

    @NotNull(message = "체크리스트 항목 상태는 필수입니다.")
    private ChecklistItemStatus status;
}