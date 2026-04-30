package com.payment.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截配置。
 *
 * 这里保留旧接口白名单，同时放开新的 v1 登录与支付回调入口。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> SaRouter.match("/**")
                .notMatch(
                        "/user/login",
                        "/user/register",
                        "/admin/login",
                        "/v1/app/auth/register",
                        "/v1/app/auth/login/password",
                        "/v1/app/auth/login/sms",
                        "/v1/app/auth/login/third-party",
                        "/v1/open/payments/**",
                        "/api/payment/notify/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/doc.html",
                        "/webjars/**",
                        "/favicon.ico"
                )
                .check(r -> StpUtil.checkLogin()))).addPathPatterns("/**");
    }
}
