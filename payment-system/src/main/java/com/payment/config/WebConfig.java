package com.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web 配置 -- 全局 CORS + 安全响应头。
 * 使用 CorsFilter（Servlet Filter 级别）而非 addCorsMappings（Interceptor 级别），
 * 确保 OPTIONS 预检请求在 Sa-Token SaInterceptor 之前获得 CORS 响应头。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 允许的跨域来源列表，可通过 cors.allowed-origins 配置 */
    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;

    /**
     * 创建全局 CORS 过滤器 Bean。
     * <p>
     * 使用 Servlet Filter 级别的 CorsFilter 而非 addCorsMappings，确保 OPTIONS 预检请求
     * 在 Sa-Token 拦截器之前获得正确的 CORS 响应头。
     *
     * @return CorsFilter 实例
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Content-Disposition");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * 创建安全响应头过滤器 Bean。
     * <p>
     * 为每个响应注入安全相关的 HTTP 头，包括：
     * <ul>
     *   <li>X-Content-Type-Options: nosniff — 防止 MIME 类型嗅探</li>
     *   <li>X-Frame-Options: DENY — 防止点击劫持</li>
     *   <li>X-XSS-Protection — 启用 XSS 过滤</li>
     *   <li>Strict-Transport-Security — 强制 HTTPS</li>
     *   <li>Content-Security-Policy — 限制资源加载来源</li>
     * </ul>
     *
     * @return OncePerRequestFilter 实例
     */
    @Bean
    public org.springframework.web.filter.OncePerRequestFilter securityHeadersFilter() {
        return new org.springframework.web.filter.OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response,
                    jakarta.servlet.FilterChain filterChain)
                    throws jakarta.servlet.ServletException, java.io.IOException {
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("X-Frame-Options", "DENY");
                response.setHeader("X-XSS-Protection", "1; mode=block");
                response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self'");
                filterChain.doFilter(request, response);
            }
        };
    }
}

