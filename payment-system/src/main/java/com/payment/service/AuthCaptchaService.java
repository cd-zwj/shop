package com.payment.service;

import com.payment.dto.LoginCaptchaVO;

/**
 * 登录验证码（图形验证码）服务接口。
 *
 * <p>负责生成和校验登录流程中使用的图形验证码，
 * 用于防范暴力破解和机器人登录，配合 {@link LoginSecurityService} 共同保障登录安全。</p>
 */
public interface AuthCaptchaService {

    /**
     * 生成图形验证码。
     *
     * <p>根据客户端 IP 生成验证码图片和唯一标识，存入 Redis 并设置过期时间，
     * 频率过高时返回需要验证码的标记。</p>
     *
     * @param clientIp 客户端 IP 地址
     * @return 验证码 VO（含图片 Base64、验证码 key、是否需要验证码等信息）
     */
    LoginCaptchaVO generateCaptcha(String clientIp);

    /**
     * 校验图形验证码。
     *
     * <p>从 Redis 中取值比对，校验通过后删除已使用的验证码，防止重放。</p>
     *
     * @param captchaKey  验证码唯一标识
     * @param captchaCode 用户输入的验证码
     * @throws com.payment.common.exception.BusinessException 验证码错误或已过期时抛出
     */
    void validateCaptcha(String captchaKey, String captchaCode);
}
