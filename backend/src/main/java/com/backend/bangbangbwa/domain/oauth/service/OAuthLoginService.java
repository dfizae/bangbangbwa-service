package com.backend.bangbangbwa.domain.oauth.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.user.dto.LoginResult;
import com.backend.bangbangbwa.domain.oauth.dto.OAuthUserInfo;
import com.backend.bangbangbwa.domain.oauth.entity.OAuthAccount;
import com.backend.bangbangbwa.domain.oauth.entity.OAuthProvider;
import com.backend.bangbangbwa.domain.user.entity.User;
import com.backend.bangbangbwa.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthLoginService {

    private final OAuthServiceProvider oauthServiceProvider;
    private final UserService userService;
    private final OAuthAccountService oauthAccountService;

    @Transactional
    public LoginResult login(
            OAuthProvider provider,
            String authorizationCode
    ) {
        OAuthService oauthService =
                oauthServiceProvider.get(provider);

        OAuthUserInfo userInfo =
                oauthService.getUserInfoByAuthorizationCode(
                        authorizationCode
                );

        Optional<OAuthAccount> optionalOAuthAccount =
                findExistingOAuthAccount(
                        provider,
                        userInfo
                );

        // 최초 가입
        if (optionalOAuthAccount.isEmpty()) {
            User user =
                    userService.createFromOAuth(userInfo);

            oauthAccountService.register(
                    user,
                    provider,
                    userInfo
            );

            return LoginResult.of(user, true);
        }

        // 기존 사용자 로그인
        OAuthAccount oauthAccount =
                optionalOAuthAccount.get();

        User user = oauthAccount.getUser();

        if (user.isDeleted()) {
            throw new CustomException(
                    ErrorCode.USER_ALREADY_DELETED
            );
        }

        return LoginResult.of(user, false);
    }

    private Optional<OAuthAccount> findExistingOAuthAccount(
            OAuthProvider provider,
            OAuthUserInfo userInfo
    ) {
        Optional<OAuthAccount> byProviderId =
                oauthAccountService.findByProviderAndProviderId(
                        provider,
                        userInfo.getProviderId()
                );

        if (byProviderId.isPresent()) {
            return byProviderId;
        }

        if (userInfo.getEmail() == null
                || userInfo.getEmail().isBlank()) {
            return Optional.empty();
        }

        return oauthAccountService.findByProviderAndEmail(
                provider,
                userInfo.getEmail()
        );
    }
}
