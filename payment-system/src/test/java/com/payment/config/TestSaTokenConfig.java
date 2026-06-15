package com.payment.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 测试用 Sa-Token 配置。
 * <p>
 * 用 in-memory 模式替代 Redis，让 MockMvc 测试不需要真实 Redis。
 * 拦截规则与生产 SaTokenConfig 保持一致。
 */
@TestConfiguration
public class TestSaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> SaRouter.match("/**")
                .notMatch(
                        "/v1/app/auth/register",
                        "/v1/app/auth/login/password",
                        "/v1/app/auth/login/sms",
                        "/v1/app/auth/login/third-party",
                        "/v1/app/auth/password/reset/send-code",
                        "/v1/app/auth/password/reset/verify",
                        "/v1/auth/captcha"
                )
                .check(r -> StpUtil.checkLogin())
        )).addPathPatterns("/**");
    }
}
