package com.backend.bangbangbwa.domain.oauth.service;

import com.backend.bangbangbwa.domain.oauth.dto.OAuthUserInfo;
import com.backend.bangbangbwa.domain.oauth.entity.OAuthProvider;

public interface OAuthService {

    OAuthProvider provider();

    OAuthUserInfo getUserInfoByAuthorizationCode(
            String authorizationCode
    );
}