package com.payment.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
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

    /** 是否启用 Swagger，启用时自动放行 API 文档路径 */
    @Value("${app.swagger.enabled:false}")
    private boolean swaggerEnabled;

    /**
     * 注册 Sa-Token 拦截器，按路径前缀区分三端认证。
     * <p>
     * 路由规则：
     * <ul>
     *   <li>/v1/admin/** — 管理端登录校验</li>
     *   <li>/v1/merchant/** — 商户端登录校验</li>
     *   <li>/v1/app/** — C 端用户登录校验</li>
     *   <li>/** — 其他路径，任意一端登录即可</li>
     * </ul>
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludePaths = new ArrayList<>(List.of(
                "/admin/login",
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

        String[] excludes = excludePaths.toArray(new String[0]);
        registry.addInterceptor(new SaInterceptor(handler -> {
            SaRouter.match("/v1/admin/**").notMatch(excludes).check(r -> AuthStpKit.ADMIN.checkLogin());
            SaRouter.match("/v1/merchant/**").notMatch(excludes).check(r -> AuthStpKit.MERCHANT.checkLogin());
            SaRouter.match("/v1/app/**").notMatch(excludes).check(r -> AuthStpKit.PLATFORM.checkLogin());
            SaRouter.match("/**")
                    .notMatch(excludes)
                    .notMatch("/v1/admin/**", "/v1/merchant/**", "/v1/app/**")
                    .check(this::checkAnyLogin);
        }).isAnnotation(true)).addPathPatterns("/**");
    }

    /**
     * 检查任意一端是否已登录。
     * <p>
     * 用于非三端前缀的通用路径，只要管理端、商户端或C端用户任一登录即可放行。
     * 若均未登录，则触发 C 端用户的登录校验（抛出 NotLoginException）。
     */
    private void checkAnyLogin() {
        if (AuthStpKit.ADMIN.isLogin() || AuthStpKit.MERCHANT.isLogin() || AuthStpKit.PLATFORM.isLogin()) {
            return;
        }
        AuthStpKit.PLATFORM.checkLogin();
    }
}
