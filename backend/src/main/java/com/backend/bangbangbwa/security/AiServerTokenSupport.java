package com.backend.bangbangbwa.security;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiServerTokenSupport {

    private static final String BEARER_PREFIX = "Bearer ";

    private final byte[] expectedToken;

    public AiServerTokenSupport(
            @Value("${app.ai-server.token:}") String expectedToken
    ) {
        this.expectedToken = expectedToken.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isAiServerRequest(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())
                || !request.getRequestURI().matches(
                        "^/api/sessions/\\d+/captures/?$"
                )) {
            return false;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization)
                || !authorization.startsWith(BEARER_PREFIX)) {
            return false;
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        return expectedToken.length > 0 && MessageDigest.isEqual(
                expectedToken,
                token.getBytes(StandardCharsets.UTF_8)
        );
    }
}
