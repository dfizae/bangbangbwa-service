package com.backend.bangbangbwa.domain.oauth.repository;

import com.backend.bangbangbwa.domain.oauth.entity.OAuthAccount;
import com.backend.bangbangbwa.domain.oauth.entity.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OAuthAccountRepository
        extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderId(
            OAuthProvider provider,
            String providerId
    );

    Optional<OAuthAccount> findByProviderAndEmail(
            OAuthProvider provider,
            String email
    );

    Optional<OAuthAccount> findFirstByUserId(
            Long userId
    );

    List<OAuthAccount> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
