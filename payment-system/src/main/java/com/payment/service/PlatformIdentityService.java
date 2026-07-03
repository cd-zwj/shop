package com.payment.service;

import com.payment.dto.PlatformRegisterDTO;
import com.payment.entity.PlatformUser;
import com.payment.service.login.PlatformLoginRequest;

/**
 * 平台用户身份服务接口。
 *
 * <p>面向平台用户（商户/管理员）提供注册、登录、认证和获取当前用户等基础身份管理能力，
 * 承接 {@code V1AdminAuthController} 和 {@code V1MerchantAuthController} 的身份认证逻辑。</p>
 */
public interface PlatformIdentityService {

    /**
     * 注册平台用户。
     *
     * <p>校验用户名和邮箱的唯一性后创建新用户，密码加密存储。</p>
     *
     * @param dto 注册请求 DTO（含用户名、密码、邮箱等）
     * @return 注册成功的平台用户信息
     * @throws com.payment.common.exception.BusinessException 用户名或邮箱已被占用时抛出
     */
    PlatformUser register(PlatformRegisterDTO dto);

    /**
     * 平台用户登录，返回 Sa-Token 令牌。
     *
     * <p>校验账号密码、登录安全策略（失败次数/锁定）和验证码（如需），
     * 登录成功后清除失败计数并生成 JWT Token。</p>
     *
     * @param request 登录请求（含账号、密码、验证码等）
     * @return Sa-Token 令牌字符串
     * @throws com.payment.common.exception.BusinessException 认证失败时抛出
     */
    String login(PlatformLoginRequest request);

    /**
     * 认证平台用户身份（不创建会话）。
     *
     * <p>仅校验账号密码是否正确并返回用户信息，不生成 Token，
     * 用于需要先认证再决定后续流程的场景。</p>
     *
     * @param request 登录请求
     * @return 认证通过的平台用户信息
     * @throws com.payment.common.exception.BusinessException 认证失败时抛出
     */
    PlatformUser authenticate(PlatformLoginRequest request);

    /**
     * 获取当前登录的平台用户信息。
     *
     * <p>基于 Sa-Token 会话从数据库查询当前登录用户的完整信息。</p>
     *
     * @return 当前登录的平台用户信息
     * @throws com.payment.common.exception.BusinessException 未登录或用户不存在时抛出
     */
    PlatformUser getCurrentUser();
}
