package com.backend.bangbangbwa.domain.oauth.service;

import com.backend.bangbangbwa.domain.user.dto.LoginResult;
import com.backend.bangbangbwa.domain.user.dto.SocialLoginRequest;
import com.backend.bangbangbwa.domain.user.dto.TokenResponse;
import com.backend.bangbangbwa.domain.oauth.entity.OAuthProvider;
import com.backend.bangbangbwa.domain.user.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final OAuthLoginService oauthLoginService;
    private final JwtService jwtService;

    public TokenResponse loginWithKakao(
            SocialLoginRequest request
    ) {
        return login(
                OAuthProvider.KAKAO,
                request.getAuthorizationCode()
        );
    }

    public TokenResponse loginWithGoogle(
            SocialLoginRequest request
    ) {
        return login(
                OAuthProvider.GOOGLE,
                request.getAuthorizationCode()
        );
    }

    private TokenResponse login(
            OAuthProvider provider,
            String authorizationCode
    ) {
        LoginResult loginResult =
                oauthLoginService.login(
                        provider,
                        authorizationCode
                );

        String serviceAccessToken =
                jwtService.createAccessToken(
                        loginResult.getUser()
                );

        return TokenResponse.of(
                serviceAccessToken,
                jwtService.getAccessTokenExpiration(),
                loginResult.isNewUser()
        );
    }
}