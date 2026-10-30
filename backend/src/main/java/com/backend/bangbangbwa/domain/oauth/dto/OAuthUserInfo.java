package com.backend.bangbangbwa.domain.oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuthUserInfo {

    private String providerId;
    private String nickname;
    private String name;
    private String email;
    private String profileImage;
}