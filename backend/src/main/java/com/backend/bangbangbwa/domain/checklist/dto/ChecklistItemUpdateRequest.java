package com.backend.bangbangbwa.domain.checklist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChecklistItemUpdateRequest {

    @NotBlank(message = "체크리스트 항목 내용은 필수입니다.")
    @Size(max = 255, message = "체크리스트 항목은 255자 이하여야 합니다.")
    private String content;

    private String memo;
}