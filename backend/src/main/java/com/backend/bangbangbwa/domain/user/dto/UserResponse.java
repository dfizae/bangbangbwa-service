package com.backend.bangbangbwa.domain.user.dto;

import com.backend.bangbangbwa.domain.oauth.entity.OAuthAccount;
import com.backend.bangbangbwa.domain.user.entity.User;
import com.backend.bangbangbwa.domain.user.entity.UserRole;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserResponse {

    private Long userId;
    private String nickname;
    private String name;
    private LocalDate birth;
    private String phone;
    private UserRole role;
    private String profileImage;
    private List<OAuthInfo> oauthAccounts;

    public static UserResponse of(
            User user,
            List<OAuthAccount> oauthAccounts
    ) {
        List<OAuthInfo> oauthInfos =
                oauthAccounts.stream()
                        .map(OAuthInfo::from)
                        .toList();

        String profileImage =
                oauthAccounts.stream()
                        .map(OAuthAccount::getProfileImage)
                        .filter(image -> image != null && !image.isBlank())
                        .findFirst()
                        .orElse(null);

        return new UserResponse(
                user.getId(),
                user.getNickname(),
                user.getName(),
                user.getBirth(),
                user.getPhone(),
                user.getRole(),
                profileImage,
                oauthInfos
        );
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class OAuthInfo {

        private String provider;
        private String email;

        public static OAuthInfo from(
                OAuthAccount oauthAccount
        ) {
            return new OAuthInfo(
                    oauthAccount.getProvider().getValue(),
                    oauthAccount.getEmail()
            );
        }
    }
}
