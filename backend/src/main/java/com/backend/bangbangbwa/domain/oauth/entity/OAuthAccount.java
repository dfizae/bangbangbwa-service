package com.backend.bangbangbwa.domain.oauth.entity;

import com.backend.bangbangbwa.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "oauth_account",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_oauth_account_provider_provider_id",
                        columnNames = {"provider", "provider_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(length = 255)
    private String email;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private OAuthAccount(
            User user,
            OAuthProvider provider,
            String providerId,
            String email,
            String profileImage
    ) {
        this.user = user;
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.profileImage = profileImage;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}