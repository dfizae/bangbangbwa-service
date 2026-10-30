package com.backend.bangbangbwa.domain.oauth.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.oauth.dto.OAuthUserInfo;
import com.backend.bangbangbwa.domain.oauth.entity.OAuthAccount;
import com.backend.bangbangbwa.domain.oauth.entity.OAuthProvider;
import com.backend.bangbangbwa.domain.user.entity.User;
import com.backend.bangbangbwa.domain.oauth.repository.OAuthAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthAccountService {

    private final OAuthAccountRepository oauthAccountRepository;

    public Optional<OAuthAccount> findByProviderAndProviderId(
            OAuthProvider provider,
            String providerId
    ) {
        return oauthAccountRepository
                .findByProviderAndProviderId(
                        provider,
                        providerId
                );
    }

    public Optional<OAuthAccount> findByProviderAndEmail(
            OAuthProvider provider,
            String email
    ) {
        return oauthAccountRepository
                .findByProviderAndEmail(
                        provider,
                        email
                );
    }

    public List<OAuthAccount> findAllByUserId(Long userId) {
        return oauthAccountRepository.findAllByUserId(userId);
    }

    @Transactional
    public OAuthAccount register(
            User user,
            OAuthProvider provider,
            OAuthUserInfo userInfo
    ) {
        OAuthAccount oauthAccount = OAuthAccount.builder()
                .user(user)
                .provider(provider)
                .providerId(userInfo.getProviderId())
                .email(userInfo.getEmail())
                .profileImage(userInfo.getProfileImage())
                .build();

        return oauthAccountRepository.save(oauthAccount);
    }

    @Transactional
    public OAuthAccount getByUserId(Long userId) {
        return oauthAccountRepository
                .findFirstByUserId(userId)
                .orElseThrow(
                        () -> new CustomException(
                                ErrorCode.OAUTH_ACCOUNT_NOT_FOUND
                        )
                );
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        oauthAccountRepository.deleteAllByUserId(userId);
    }
}
