package com.backend.bangbangbwa.domain.oauth.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.oauth.entity.OAuthProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class OAuthServiceProvider {

    private final Map<OAuthProvider, OAuthService> oauthServiceMap;

    public OAuthServiceProvider(List<OAuthService> oauthServices) {
        Map<OAuthProvider, OAuthService> map =
                new EnumMap<>(OAuthProvider.class);

        for (OAuthService oauthService : oauthServices) {
            OAuthProvider provider = oauthService.provider();

            if (map.put(provider, oauthService) != null) {
                throw new IllegalStateException(
                        "중복된 OAuthService 구현체가 존재합니다: " + provider
                );
            }
        }

        this.oauthServiceMap = Map.copyOf(map);
    }

    public OAuthService get(OAuthProvider provider) {
        OAuthService oauthService =
                oauthServiceMap.get(provider);

        if (oauthService == null) {
            throw new CustomException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        return oauthService;
    }
}
