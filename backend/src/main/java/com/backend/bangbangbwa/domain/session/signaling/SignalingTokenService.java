package com.backend.bangbangbwa.domain.session.signaling;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SignalingTokenService {

    private static final String TOKEN_TYPE = "SIGNALING";
    private static final Duration EXPIRATION = Duration.ofMinutes(5);

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public String createToken(Long userId, String roomToken) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("bangbangbwa")
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiresAt(now.plus(EXPIRATION))
                .claim("tokenType", TOKEN_TYPE)
                .claim("roomToken", roomToken)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public SignalingPrincipal parseAndValidate(
            String token,
            String requestedRoomToken
    ) {
        Jwt jwt = jwtDecoder.decode(token);

        String tokenType = jwt.getClaimAsString("tokenType");
        String tokenRoomToken = jwt.getClaimAsString("roomToken");

        if (!TOKEN_TYPE.equals(tokenType)) {
            throw new IllegalArgumentException("시그널링 전용 토큰이 아닙니다.");
        }

        if (!requestedRoomToken.equals(tokenRoomToken)) {
            throw new IllegalArgumentException("토큰의 RTC 방 정보가 일치하지 않습니다.");
        }

        return new SignalingPrincipal(
                Long.valueOf(jwt.getSubject()),
                tokenRoomToken
        );
    }

    public record SignalingPrincipal(Long userId, String roomToken) {
    }
}
