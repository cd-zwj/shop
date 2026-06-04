package com.payment.service;

import com.payment.dto.RecoveredPlatformAccountVO;
import com.payment.entity.PlatformUser;

/**
 * 平台邮箱账号服务接口，用于定义平台邮箱账号相关业务能力。
 */
public interface PlatformEmailAccountService {

    /**
     * 向已绑定且可用的邮箱发送登录验证码。
     */
    void sendLoginCode(String email);

    /**
     * 向已绑定邮箱发送账号找回和密码重置验证码。
     */
    void sendRecoverCode(String email);

    /**
     * 为指定平台用户发送绑定邮箱验证码。
     */
    void sendBindCode(Long platformUserId, String email);

    /**
     * 校验绑定验证码并把邮箱绑定到指定平台用户。
     */
    PlatformUser bindEmail(Long platformUserId, String email, String emailCode);

    /**
     * 校验找回验证码并返回邮箱绑定的账号信息。
     */
    RecoveredPlatformAccountVO recoverAccount(String email, String emailCode);

    /**
     * 校验找回验证码并重置邮箱绑定账号的密码。
     */
    void resetPassword(String email, String emailCode, String newPassword);
}
