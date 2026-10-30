package com.backend.bangbangbwa.domain.user.dto;

import com.backend.bangbangbwa.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginResult {

    private User user;
    private boolean newUser;

    public static LoginResult of(User user, boolean newUser) {
        return new LoginResult(user, newUser);
    }
}
