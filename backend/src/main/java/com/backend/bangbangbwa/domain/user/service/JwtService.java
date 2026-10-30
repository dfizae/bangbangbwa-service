package com.backend.bangbangbwa.domain.user.service;

import com.backend.bangbangbwa.domain.user.entity.User;
import com.backend.bangbangbwa.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtTokenProvider jwtTokenProvider;

    public String createAccessToken(User user) {
        return jwtTokenProvider.createAccessToken(user);
    }

    public long getAccessTokenExpiration() {
        return jwtTokenProvider.getAccessTokenExpiration();
    }
}
