package com.payment.service;

/**
 * 短信验证码服务接口。
 *
 * <p>负责登录场景下短信验证码的发送、重发和校验，
 * 验证码存储于 Redis 并设置过期时间，内置发送频率限制。</p>
 */
public interface SmsCodeService {

    /**
     * 发送登录短信验证码。
     *
     * <p>生成 6 位随机验证码，通过短信网关发送到指定手机号，
     * 同时存入 Redis 并设置过期时间和发送频率限制。</p>
     *
     * @param phone 手机号
     * @throws com.payment.common.exception.BusinessException 发送频率过高时抛出
     */
    void sendLoginCode(String phone);

    /**
     * 重发 Redis 中仍有效的登录验证码。
     *
     * <p>用于短信发送失败后的重试场景，不重新生成验证码，
     * 仅将 Redis 中已存在的验证码重新通过短信网关发送。供补偿任务调用。</p>
     *
     * @param phone 手机号
     * @throws com.payment.common.exception.BusinessException 无有效验证码可重发时抛出
     */
    void retryLoginCode(String phone);

    /**
     * 校验登录短信验证码。
     *
     * <p>从 Redis 中取值比对，校验通过后根据 consume 参数决定是否删除已使用的验证码。</p>
     *
     * @param phone   手机号
     * @param code    用户输入的验证码
     * @param consume 是否消费（{@code true} 校验后删除，{@code false} 仅校验不删除）
     * @throws com.payment.common.exception.BusinessException 验证码错误或已过期时抛出
     */
    void validateLoginCode(String phone, String code, boolean consume);
}
