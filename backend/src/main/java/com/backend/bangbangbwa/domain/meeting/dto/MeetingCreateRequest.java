package com.backend.bangbangbwa.domain.meeting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class MeetingCreateRequest {

    @NotNull(message = "희망 회의 시간은 필수입니다.")
    @Size(
            min = 1,
            max = 3,
            message = "희망 회의 시간은 1개 이상 3개 이하로 입력해야 합니다."
    )
    private List<
            @NotNull(message = "희망 회의 시간은 null일 수 없습니다.")
            @Future(message = "희망 회의 시간은 현재 시간 이후여야 합니다.")
                    LocalDateTime
            > preferredTimes;
}
