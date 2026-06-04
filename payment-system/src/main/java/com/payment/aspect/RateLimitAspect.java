package com.payment.aspect;

import cn.hutool.core.util.StrUtil;
import com.payment.annotation.RateLimit;
import com.payment.common.BusinessException;
import com.payment.util.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 接口限流切面，用于拦截并执行接口限流逻辑。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String DEFAULT_KEY = "anonymous";

    private final RedisUtils redisUtils;

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 执行接口限流校验。
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String ip = normalizeKey(resolveClientIp());
        String evaluatedKey = evaluateKey(rateLimit.key(), method, joinPoint.getArgs());
        String primaryKey = StrUtil.blankToDefault(evaluatedKey, DEFAULT_KEY);

        StringBuilder finalKey = new StringBuilder(rateLimit.prefix()).append(":").append(primaryKey);
        if (rateLimit.includeIp()) {
            finalKey.append(":").append(ip);
        }

        long current = redisUtils.incrementAndGet(finalKey.toString(), rateLimit.window(), rateLimit.unit());
        if (current > rateLimit.maxRequests()) {
            throw new BusinessException(rateLimit.message());
        }

        return joinPoint.proceed();
    }

    /**
     * 解析限流键表达式。
     */
    private String evaluateKey(String expression, Method method, Object[] args) {
        if (StrUtil.isBlank(expression)) {
            return null;
        }
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                null,
                method,
                args,
                parameterNameDiscoverer
        );
        Object value = expressionParser.parseExpression(expression).getValue(context);
        return value == null ? null : normalizeKey(String.valueOf(value));
    }

    /**
     * 获取客户端 IP（仅信任 RemoteAddr，防止 X-Forwarded-For 伪造绕过限流）。
     */
    private String resolveClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return DEFAULT_KEY;
        }
        HttpServletRequest request = attributes.getRequest();
        return StrUtil.blankToDefault(request.getRemoteAddr(), DEFAULT_KEY);
    }

    /**
     * 规范化限流键。
     */
    private String normalizeKey(String value) {
        if (StrUtil.isBlank(value)) {
            return DEFAULT_KEY;
        }
        return value.trim()
                .toLowerCase()
                .replace(":", "_")
                .replace(".", "_")
                .replace("@", "_at_")
                .replace(" ", "_");
    }
}
