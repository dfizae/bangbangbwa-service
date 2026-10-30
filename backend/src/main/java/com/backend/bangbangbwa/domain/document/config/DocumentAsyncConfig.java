package com.backend.bangbangbwa.domain.document.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 매물 서류 AI 분석을 백그라운드로 처리하기 위해 @Async 를 활성화합니다.
 * (Spring Boot 기본 TaskExecutor 사용)
 */
@Configuration
@EnableAsync
public class DocumentAsyncConfig {
}
