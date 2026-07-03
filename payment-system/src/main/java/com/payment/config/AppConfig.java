package com.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

/**
 * 应用配置类
 */
@Configuration
@EnableAsync
public class AppConfig {
    
    /**
     * 创建 RestTemplate Bean，用于服务间 HTTP 调用。
     *
     * @return RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

