package com.payment.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试用 Sa-Token 配置。
 * <p>
 * 用 in-memory 模式替代 Redis，让 MockMvc 测试不需要真实 Redis。
 * 拦截规则与生产 SaTokenConfig 保持一致：三端分离鉴权。
 */
@TestConfiguration
public class TestSaTokenConfig implements WebMvcConfigurer {
    @Bean
    @Primary
    public SaTokenDao testSaTokenDao() {
        SaTokenDaoDefaultImpl dao = new SaTokenDaoDefaultImpl();
        dao.init();
        return dao;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludePaths = List.of(
                "/v1/auth/captcha",
                "/v1/app/auth/register",
                "/v1/app/auth/login/password",
                "/v1/app/auth/login/sms",
                "/v1/app/auth/login/third-party",
                "/v1/app/auth/password/reset/send-code",
                "/v1/app/auth/password/reset/verify",
                "/v1/admin/auth/login",
                "/v1/merchant/auth/login",
                "/v1/open/payments/**",
                "/payment/notify/**",
                "/actuator/**",
                "/favicon.ico"
        );

        String[] excludes = excludePaths.toArray(new String[0]);
        registry.addInterceptor(new SaInterceptor(handler -> {
            SaRouter.match("/v1/admin/**").notMatch(excludes).check(r -> AuthStpKit.ADMIN.checkLogin());
            SaRouter.match("/v1/merchant/**").notMatch(excludes).check(r -> AuthStpKit.MERCHANT.checkLogin());
            SaRouter.match("/v1/app/**").notMatch(excludes).check(r -> AuthStpKit.PLATFORM.checkLogin());
            SaRouter.match("/**")
                    .notMatch(excludes)
                    .notMatch("/v1/admin/**", "/v1/merchant/**", "/v1/app/**")
                    .check(r -> {
                        if (AuthStpKit.ADMIN.isLogin() || AuthStpKit.MERCHANT.isLogin() || AuthStpKit.PLATFORM.isLogin()) {
                            return;
                        }
                        AuthStpKit.PLATFORM.checkLogin();
                    });
        }).isAnnotation(true)).addPathPatterns("/**");
    }
}
