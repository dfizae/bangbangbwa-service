package com.backend.bangbangbwa.domain.oauth.service;

import com.backend.bangbangbwa.common.exception.CustomException;
import com.backend.bangbangbwa.common.exception.ErrorCode;
import com.backend.bangbangbwa.domain.oauth.dto.OAuthUserInfo;
import com.backend.bangbangbwa.domain.oauth.entity.OAuthProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService implements OAuthService {

    private static final String KAKAO_TOKEN_URL =
            "https://kauth.kakao.com/oauth/token";

    private static final String KAKAO_USER_INFO_URL =
            "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.client-secret:}")
    private String clientSecret;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo getUserInfoByAuthorizationCode(
            String authorizationCode
    ) {
        String accessToken =
                getAccessToken(
                        authorizationCode
                );

        return getUserInfo(accessToken);
    }

    private String getAccessToken(
            String authorizationCode
    ) {
        MultiValueMap<String, String> formData =
                new LinkedMultiValueMap<>();

        formData.add(
                "grant_type",
                "authorization_code"
        );
        formData.add(
                "client_id",
                clientId
        );
        formData.add(
                "redirect_uri",
                redirectUri
        );
        formData.add(
                "code",
                authorizationCode
        );

        if (clientSecret != null
                && !clientSecret.isBlank()) {
            formData.add(
                    "client_secret",
                    clientSecret
            );
        }

        try {
            KakaoTokenResponse response =
                    restClient.post()
                            .uri(KAKAO_TOKEN_URL)
                            .contentType(
                                    MediaType.APPLICATION_FORM_URLENCODED
                            )
                            .body(formData)
                            .retrieve()
                            .onStatus(
                                    HttpStatusCode::isError,
                                    (request, responseStatus) -> {
                                        log.warn(
                                                "카카오 Access Token 발급 실패. status={}",
                                                responseStatus.getStatusCode()
                                        );

                                        throw new CustomException(
                                                ErrorCode.INVALID_SOCIAL_TOKEN
                                        );
                                    }
                            )
                            .body(KakaoTokenResponse.class);

            if (response == null
                    || response.accessToken() == null
                    || response.accessToken().isBlank()) {
                throw new CustomException(
                        ErrorCode.INVALID_SOCIAL_TOKEN
                );
            }

            return response.accessToken();

        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn(
                    "카카오 Access Token 요청 중 통신 오류가 발생했습니다.",
                    exception
            );

            throw new CustomException(
                    ErrorCode.INVALID_SOCIAL_TOKEN
            );
        }
    }

    private OAuthUserInfo getUserInfo(
            String accessToken
    ) {
        try {
            KakaoUserInfoResponse response =
                    restClient.get()
                            .uri(KAKAO_USER_INFO_URL)
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + accessToken
                            )
                            .retrieve()
                            .onStatus(
                                    HttpStatusCode::isError,
                                    (request, responseStatus) -> {
                                        log.warn(
                                                "카카오 사용자 정보 조회 실패. status={}",
                                                responseStatus.getStatusCode()
                                        );

                                        throw new CustomException(
                                                ErrorCode.INVALID_SOCIAL_TOKEN
                                        );
                                    }
                            )
                            .body(KakaoUserInfoResponse.class);

            if (response == null
                    || response.id() == null) {
                throw new CustomException(
                        ErrorCode.INVALID_SOCIAL_TOKEN
                );
            }

            KakaoAccount account =
                    response.kakaoAccount();

            KakaoProfile profile =
                    account == null
                            ? null
                            : account.profile();

            String nickname =
                    profile == null
                            ? null
                            : profile.nickname();

            String profileImage =
                    profile == null
                            ? null
                            : profile.profileImageUrl();

            String email =
                    account == null
                            ? null
                            : account.email();

            return new OAuthUserInfo(
                    String.valueOf(response.id()),
                    nickname,
                    nickname,
                    email,
                    profileImage
            );

        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn(
                    "카카오 사용자 정보 요청 중 통신 오류가 발생했습니다.",
                    exception
            );

            throw new CustomException(
                    ErrorCode.INVALID_SOCIAL_TOKEN
            );
        }
    }

    private record KakaoTokenResponse(
            @JsonProperty("token_type")
            String tokenType,

            @JsonProperty("access_token")
            String accessToken,

            @JsonProperty("expires_in")
            Long expiresIn,

            @JsonProperty("refresh_token")
            String refreshToken,

            @JsonProperty("refresh_token_expires_in")
            Long refreshTokenExpiresIn,

            String scope
    ) {
    }

    private record KakaoUserInfoResponse(
            Long id,

            @JsonProperty("kakao_account")
            KakaoAccount kakaoAccount
    ) {
    }

    private record KakaoAccount(
            String email,
            KakaoProfile profile
    ) {
    }

    private record KakaoProfile(
            String nickname,

            @JsonProperty("profile_image_url")
            String profileImageUrl
    ) {
    }
}