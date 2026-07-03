package com.payment.service;

import com.payment.enums.EmailCodeSceneEnum;

/**
 * 邮箱验证码服务接口。
 *
 * <p>负责邮箱验证码的发送和校验，支持多业务场景（登录、注册、找回密码、绑定邮箱等），
 * 内置发送频率限制和验证码有效期管理。</p>
 */
public interface EmailCodeService {

    /**
     * 发送邮箱验证码。
     *
     * <p>根据业务场景生成验证码并通过邮件发送，同时存入 Redis 并设置过期时间。
     * 内置发送频率限制，短时间内重复调用将被拒绝。</p>
     *
     * @param email  目标邮箱地址
     * @param scene  业务场景枚举（LOGIN / REGISTER / RECOVER / BIND 等）
     * @throws com.payment.common.exception.BusinessException 发送频率过高时抛出
     */
    void sendCode(String email, EmailCodeSceneEnum scene);

    /**
     * 校验邮箱验证码。
     *
     * <p>从 Redis 中取值比对，校验通过后根据 consume 参数决定是否删除已使用的验证码。</p>
     *
     * @param email   目标邮箱地址
     * @param code    用户输入的验证码
     * @param scene   业务场景枚举
     * @param consume 是否消费（{@code true} 校验后删除，{@code false} 仅校验不删除）
     * @throws com.payment.common.exception.BusinessException 验证码错误或已过期时抛出
     */
    void validateCode(String email, String code, EmailCodeSceneEnum scene, boolean consume);
}
