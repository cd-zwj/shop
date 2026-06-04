package com.payment.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解，用于标记需要执行限流的接口。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 定义限流前缀。
     */
    String prefix();

    /**
     * 定义限流键。
     */
    String key() default "";

    /**
     * 定义限流时间窗口。
     */
    long window();

    /**
     * 定义时间窗口内的最大请求次数。
     */
    long maxRequests();

    /**
     * 定义限流提示信息。
     */
    String message() default "请求过于频繁，请稍后再试";

    /**
     * 定义限流是否包含客户端 IP。
     */
    boolean includeIp() default false;

    /**
     * 定义限流时间单位。
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}
