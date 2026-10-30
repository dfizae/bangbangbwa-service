package com.backend.bangbangbwa.domain.oauth.entity;

import lombok.Getter;

@Getter
public enum OAuthProvider {
    KAKAO("kakao"),
    GOOGLE("google");

    private final String value;

    OAuthProvider(String value) {
        this.value = value;
    }

}
