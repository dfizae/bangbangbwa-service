package com.backend.bangbangbwa.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiAwareBearerTokenResolver implements BearerTokenResolver {

    private final AiServerTokenSupport tokenSupport;
    private final DefaultBearerTokenResolver delegate =
            new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        if (tokenSupport.isAiServerRequest(request)) {
            return null;
        }
        return delegate.resolve(request);
    }
}
