package com.payment.service;

import com.payment.enums.EmailCodeSceneEnum;

/**
 * 邮箱验证码服务接口，用于定义邮箱验证码相关业务能力。
 */
public interface EmailCodeService {

    /**
     * 发送编码。
     */
    void sendCode(String email, EmailCodeSceneEnum scene);

    /**
     * 校验编码。
     */
    void validateCode(String email, String code, EmailCodeSceneEnum scene, boolean consume);
}
