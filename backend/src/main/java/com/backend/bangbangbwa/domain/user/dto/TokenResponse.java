package com.backend.bangbangbwa.domain.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TokenResponse {

    private String tokenType;
    private String accessToken;
    private long expiresIn;
    private boolean newUser;

    public static TokenResponse of(
            String accessToken,
            long expiresIn,
            boolean newUser
    ) {
        return new TokenResponse(
                "Bearer",
                accessToken,
                expiresIn,
                newUser
        );
    }
}