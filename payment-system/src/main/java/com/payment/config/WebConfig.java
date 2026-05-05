package com.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * Sa-Token的拦截器配置已移动到 SaTokenConfig.java
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 原有的 JwtAuthInterceptor 已移除，使用 Sa-Token 接管鉴权
}

