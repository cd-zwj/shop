package com.payment.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 拦截配置。
 *
 * 这里保留旧接口白名单，同时放开新的 v1 登录与支付回调入口。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Value("${app.swagger.enabled:false}")
    private boolean swaggerEnabled;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludePaths = new ArrayList<>(List.of(
                "/admin/login",
                "/v1/auth/captcha",
                "/v1/app/auth/register",
                "/v1/app/auth/login/password",
                "/v1/app/auth/login/sms",
                "/v1/app/auth/login/third-party",
                "/v1/admin/auth/login",
                "/v1/merchant/auth/login",
                "/v1/open/payments/**",
                "/api/payment/notify/**",
                "/actuator/**",
                "/favicon.ico"
        ));

        if (swaggerEnabled) {
            excludePaths.addAll(List.of(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/doc.html",
                    "/webjars/**"
            ));
        }

        registry.addInterceptor(new SaInterceptor(handler -> SaRouter.match("/**")
                .notMatch(excludePaths.toArray(new String[0]))
                .check(r -> StpUtil.checkLogin()))).addPathPatterns("/**");
    }
}
