package com.payment.rag.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
@ConfigurationProperties(prefix = "video")
@Data
public class VideoConfig {
    private int segmentDuration = 30;  // seconds
    private int keyframeInterval = 5;   // seconds
    private int threadPoolSize = 4;     // 核心线程数
    private int maxThreadPoolSize = 10; // 最大线程数
    private int queueCapacity = 1000;   // 队列容量

    /**
     * 视频处理线程池（优化版）
     * 修复：使用有界队列 + 拒绝策略，避免无界队列导致 OOM
     */
    @Bean(name = "videoProcessorExecutor", destroyMethod = "shutdown")
    public ExecutorService videoProcessorExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            threadPoolSize,                          // 核心线程数
            maxThreadPoolSize,                       // 最大线程数
            60L,                                     // 空闲线程存活时间
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queueCapacity), // 有界队列，防止 OOM
            new ThreadFactory() {
                private int count = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("video-processor-" + count++);
                    thread.setDaemon(false);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 队列满时，由调用线程执行
        );

        // 允许核心线程超时
        executor.allowCoreThreadTimeOut(true);

        return executor;
    }
}
