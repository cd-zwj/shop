package com.payment.service;

import com.payment.dto.RecoveredPlatformAccountVO;
import com.payment.entity.PlatformUser;

/**
 * 平台邮箱账号服务接口。
 *
 * <p>面向平台用户（商户/管理员）提供基于邮箱的验证码登录、
 * 账号找回、密码重置和邮箱绑定能力。
 * 验证码的生成和发送委托给 {@link EmailCodeService} 处理。</p>
 */
public interface PlatformEmailAccountService {

    /**
     * 向已绑定且可用的邮箱发送登录验证码。
     *
     * @param email 已注册的邮箱地址
     * @throws com.payment.common.exception.BusinessException 邮箱未注册或账号被禁用时抛出
     */
    void sendLoginCode(String email);

    /**
     * 向已绑定邮箱发送账号找回和密码重置验证码。
     *
     * @param email 已注册的邮箱地址
     * @throws com.payment.common.exception.BusinessException 邮箱未注册时抛出
     */
    void sendRecoverCode(String email);

    /**
     * 为指定平台用户发送绑定邮箱验证码。
     *
     * <p>验证码发送到待绑定的新邮箱，校验后完成绑定。</p>
     *
     * @param platformUserId 平台用户ID
     * @param email          待绑定的邮箱地址
     * @throws com.payment.common.exception.BusinessException 邮箱已被其他用户绑定时抛出
     */
    void sendBindCode(Long platformUserId, String email);

    /**
     * 校验绑定验证码并完成邮箱绑定。
     *
     * @param platformUserId 平台用户ID
     * @param email          待绑定的邮箱地址
     * @param emailCode      用户输入的验证码
     * @return 绑定后的平台用户信息
     * @throws com.payment.common.exception.BusinessException 验证码错误或已过期时抛出
     */
    PlatformUser bindEmail(Long platformUserId, String email, String emailCode);

    /**
     * 校验找回验证码并返回邮箱绑定的账号信息。
     *
     * <p>用于用户找回账号场景，验证成功后返回账号摘要信息供用户确认。</p>
     *
     * @param email     已注册的邮箱地址
     * @param emailCode 用户输入的验证码
     * @return 找回的平台账号摘要信息
     * @throws com.payment.common.exception.BusinessException 验证码错误或已过期时抛出
     */
    RecoveredPlatformAccountVO recoverAccount(String email, String emailCode);

    /**
     * 校验找回验证码并重置密码。
     *
     * @param email       已注册的邮箱地址
     * @param emailCode   用户输入的验证码
     * @param newPassword 新密码
     * @throws com.payment.common.exception.BusinessException 验证码错误或已过期时抛出
     */
    void resetPassword(String email, String emailCode, String newPassword);
}
