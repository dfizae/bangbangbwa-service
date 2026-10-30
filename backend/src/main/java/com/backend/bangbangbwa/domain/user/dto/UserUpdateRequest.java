package com.backend.bangbangbwa.domain.user.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    private String nickname;

    private LocalDate birth;

    @Pattern(
            regexp = "^010-\\d{4}-\\d{4}$",
            message = "휴대폰 번호 형식은 010-1234-5678 입니다."
    )
    private String phone;
}
