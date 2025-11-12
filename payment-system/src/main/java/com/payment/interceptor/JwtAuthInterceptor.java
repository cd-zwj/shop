package com.payment.interceptor;

import com.payment.annotation.RequireAuth;
import com.payment.common.BusinessException;
import com.payment.common.ResultCode;
import com.payment.entity.Tenant;
import com.payment.mapper.TenantMapper;
import com.payment.util.JwtUtil;
import com.payment.util.TenantContextHolder;
import com.payment.util.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * JWT认证拦截器
 */
@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private TenantMapper tenantMapper;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是映射到方法，直接通过
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Method method = handlerMethod.getMethod();
        
        // 检查方法或类上是否有@RequireAuth注解
        RequireAuth requireAuth = method.getAnnotation(RequireAuth.class);
        if (requireAuth == null) {
            requireAuth = method.getDeclaringClass().getAnnotation(RequireAuth.class);
        }
        
        // 如果没有@RequireAuth注解，直接通过
        if (requireAuth == null) {
            return true;
        }
        
        // 从请求头中获取token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "未登录，请先登录");
        }
        
        // 移除Bearer前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 验证token
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "登录已过期，请重新登录");
        }
        
        // 将用户ID和用户名存入ThreadLocal
        Long userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        Long tenantId = jwtUtil.getTenantIdFromToken(token);
        String tenantCode = jwtUtil.getTenantCodeFromToken(token);
        
        UserContextHolder.setUserId(userId);
        UserContextHolder.setUsername(username);
        UserContextHolder.setToken(token);
        
        // 设置租户上下文
        if (tenantId != null) {
            TenantContextHolder.setTenantId(tenantId);
            
            // 验证商家状态（禁用商家无法登录和访问接口）
            Tenant tenant = tenantMapper.selectById(tenantId);
            if (tenant == null || tenant.getDeleted() == 1) {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "商家不存在");
            }
            if (tenant.getStatus() == 0) {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "商家已被禁用，无法访问系统");
            }
        }
        if (tenantCode != null) {
            TenantContextHolder.setTenantCode(tenantCode);
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后清除ThreadLocal，防止内存泄漏
        UserContextHolder.clear();
        TenantContextHolder.clear();
    }
}

