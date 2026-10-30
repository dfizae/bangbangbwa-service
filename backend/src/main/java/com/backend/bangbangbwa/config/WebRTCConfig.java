package com.backend.bangbangbwa.config;

import com.backend.bangbangbwa.domain.session.signaling.SignalHandler;
import com.backend.bangbangbwa.domain.session.signaling.SignalingHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;


@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebRTCConfig implements WebSocketConfigurer {

    private final SignalHandler signalHandler;
    private final SignalingHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalHandler, "/ws/signaling")
                .addInterceptors(handshakeInterceptor)
                //TODO : 프론트 주소만 허용하는 것으로 수정하기
                .setAllowedOriginPatterns("*");
    }

    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container =
                new ServletServerContainerFactoryBean();

        container.setMaxTextMessageBufferSize(64 * 1024);
        container.setMaxBinaryMessageBufferSize(64 * 1024);
        container.setMaxSessionIdleTimeout(30 * 60 * 1000L);
        return container;
    }
}
