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

@Service
@RequiredArgsConstructor
public class GoogleOAuthService implements OAuthService {

    private static final String GOOGLE_TOKEN_URL =
            "https://oauth2.googleapis.com/token";

    private static final String GOOGLE_USER_INFO_URL =
            "https://openidconnect.googleapis.com/v1/userinfo";

    private final RestClient restClient;

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String redirectUri;

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo getUserInfoByAuthorizationCode(
            String authorizationCode
    ) {
        String accessToken =
                exchangeAuthorizationCode(
                        authorizationCode
                );

        return getUserInfo(accessToken);
    }

    private String exchangeAuthorizationCode(
            String authorizationCode
    ) {
        try {
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
                    "client_secret",
                    clientSecret
            );
            formData.add(
                    "redirect_uri",
                    redirectUri
            );
            formData.add(
                    "code",
                    authorizationCode
            );

            GoogleTokenResponse response =
                    restClient.post()
                            .uri(GOOGLE_TOKEN_URL)
                            .contentType(
                                    MediaType.APPLICATION_FORM_URLENCODED
                            )
                            .body(formData)
                            .retrieve()
                            .onStatus(
                                    HttpStatusCode::isError,
                                    (request, responseStatus) -> {
                                        throw new CustomException(
                                                ErrorCode.INVALID_SOCIAL_TOKEN
                                        );
                                    }
                            )
                            .body(GoogleTokenResponse.class);

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
            throw new CustomException(
                    ErrorCode.INVALID_SOCIAL_TOKEN
            );
        }
    }

    private OAuthUserInfo getUserInfo(
            String accessToken
    ) {
        try {
            GoogleUserInfoResponse response =
                    restClient.get()
                            .uri(GOOGLE_USER_INFO_URL)
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + accessToken
                            )
                            .retrieve()
                            .onStatus(
                                    HttpStatusCode::isError,
                                    (request, responseStatus) -> {
                                        throw new CustomException(
                                                ErrorCode.INVALID_SOCIAL_TOKEN
                                        );
                                    }
                            )
                            .body(GoogleUserInfoResponse.class);

            if (response == null
                    || response.providerId() == null
                    || response.providerId().isBlank()) {
                throw new CustomException(
                        ErrorCode.INVALID_SOCIAL_TOKEN
                );
            }

            return new OAuthUserInfo(
                    response.providerId(),
                    response.name(),
                    response.name(),
                    response.email(),
                    response.picture()
            );

        } catch (CustomException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new CustomException(
                    ErrorCode.INVALID_SOCIAL_TOKEN
            );
        }
    }

    private record GoogleTokenResponse(
            @JsonProperty("access_token")
            String accessToken,

            @JsonProperty("expires_in")
            Long expiresIn,

            @JsonProperty("refresh_token")
            String refreshToken,

            String scope,

            @JsonProperty("token_type")
            String tokenType,

            @JsonProperty("id_token")
            String idToken
    ) {
    }

    private record GoogleUserInfoResponse(
            @JsonProperty("sub")
            String providerId,

            String name,
            String email,
            String picture
    ) {
    }
}