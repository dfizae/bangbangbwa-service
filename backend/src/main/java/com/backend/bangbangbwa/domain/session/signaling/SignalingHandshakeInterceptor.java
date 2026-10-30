package com.backend.bangbangbwa.domain.session.signaling;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SignalingHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ROOM_TOKEN = "roomToken";
    public static final String USER_ID = "userId";
    private static final String ACCESS_TOKEN = "accessToken";

    private final SignalingTokenService signalingTokenService;
    private final SignalingAccessService signalingAccessService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(request.getURI())
                .build()
                .getQueryParams();

        String roomToken = params.getFirst(ROOM_TOKEN);
        String accessToken = params.getFirst(ACCESS_TOKEN);

        if (!hasText(roomToken) || !hasText(accessToken)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            SignalingTokenService.SignalingPrincipal principal =
                    signalingTokenService.parseAndValidate(
                            accessToken,
                            roomToken
                    );

            // 토큰이 유효하더라도 현재 세션의 실제 참여자인지 다시 확인합니다.
            signalingAccessService.validateAccess(
                    roomToken,
                    principal.userId()
            );

            attributes.put(ROOM_TOKEN, roomToken);
            attributes.put(USER_ID, principal.userId());
            return true;
        } catch (RuntimeException exception) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // 후처리 없음
    }
}
