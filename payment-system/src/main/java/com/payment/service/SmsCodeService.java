package com.payment.service;

/**
 * 短信验证码服务接口，用于定义短信验证码相关业务能力。
 */
public interface SmsCodeService {

    /**
     * 发送登录验证码。
     */
    void sendLoginCode(String phone);

    /**
     * 重发 Redis 中仍有效的登录验证码，供重试任务调用。
     */
    void retryLoginCode(String phone);

    /**
     * 校验登录验证码。
     */
    void validateLoginCode(String phone, String code, boolean consume);
}
