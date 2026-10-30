package com.backend.bangbangbwa.security;

import com.backend.bangbangbwa.domain.user.entity.User;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Getter
@Component
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final long accessTokenExpiration;

    public JwtTokenProvider(
            JwtEncoder jwtEncoder,
            @Value("${jwt.access-token-expiration}")
            long accessTokenExpiration
    ) {
        this.jwtEncoder = jwtEncoder;
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("bangbangbwa")
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(
                        now.plus(
                                Duration.ofMillis(
                                        accessTokenExpiration
                                )
                        )
                )
                .claim("tokenType", "ACCESS")
                .claim("role", user.getRole().name())
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}