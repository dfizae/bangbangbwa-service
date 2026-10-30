package com.backend.bangbangbwa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String REPORT_EXECUTOR = "reportTaskExecutor";

    /**
     * 리포트 비동기 조립 전용 스레드풀.
     *
     * <p>조립은 OpenAI 호출·DB 위주의 I/O 바운드 작업이므로 스레드 수는 작게 유지하고,
     * 큐를 유한하게 두어 폭주 시 메모리 무한 증가를 막는다. 큐까지 가득 차면
     * {@link ThreadPoolExecutor.CallerRunsPolicy}로 호출 스레드가 직접 실행해 유입을 자연스럽게 감속한다.
     */
    @Bean(REPORT_EXECUTOR)
    public Executor reportTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("report-async-");
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
