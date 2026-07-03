package com.payment.rag.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置，统一管理 @Async 线程池和优雅停机。
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * 通用异步任务线程池（画像提炼、后台通知等）。
     * <p>
     * 配置了 graceful shutdown：应用关闭时等待队列中的任务执行完毕再终止。
     */
    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            RequestAttributes capturedRequestAttributes = RequestContextHolder.getRequestAttributes();
            Map<String, String> capturedMdc = MDC.getCopyOfContextMap();
            return () -> {
                RequestAttributes previousRequestAttributes = RequestContextHolder.getRequestAttributes();
                Map<String, String> previousMdc = MDC.getCopyOfContextMap();
                try {
                    if (capturedRequestAttributes != null) {
                        RequestContextHolder.setRequestAttributes(capturedRequestAttributes);
                    } else {
                        RequestContextHolder.resetRequestAttributes();
                    }
                    if (capturedMdc != null) {
                        MDC.setContextMap(capturedMdc);
                    } else {
                        MDC.clear();
                    }
                    runnable.run();
                } finally {
                    if (previousRequestAttributes != null) {
                        RequestContextHolder.setRequestAttributes(previousRequestAttributes);
                    } else {
                        RequestContextHolder.resetRequestAttributes();
                    }
                    if (previousMdc != null) {
                        MDC.setContextMap(previousMdc);
                    } else {
                        MDC.clear();
                    }
                }
            };
        };
    }

    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        return createTaskExecutor("async-task-");
    }

    @Bean(name = "mvcTaskExecutor")
    public Executor mvcTaskExecutor() {
        return createTaskExecutor("mvc-task-");
    }

    private Executor createTaskExecutor(String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(64);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setTaskDecorator(mdcTaskDecorator());
        executor.initialize();
        return executor;
    }
}
