package com.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payment.dto.LoginDTO;
import com.payment.dto.MiniProgramUserVO;
import com.payment.dto.WechatLoginDTO;
import com.payment.entity.User;

/**
 * C 端用户服务接口。
 *
 * <p>面向 C 端终端用户提供登录、注册和第三方登录等基础用户管理能力，
 * 继承 MyBatis-Plus 的 {@link IService} 获得通用 CRUD 能力。
 * 承接 {@code V1AppAuthController} 的身份认证逻辑。</p>
 */
public interface UserService extends IService<User> {

    /**
     * C 端用户登录（用户名/手机号 + 密码）。
     *
     * @param dto 登录请求 DTO（含账号和密码）
     * @return Sa-Token 令牌字符串
     * @throws com.payment.common.exception.BusinessException 认证失败时抛出
     */
    String login(LoginDTO dto);

    /**
     * 管理端用户登录（管理员账号 + 密码）。
     *
     * <p>与 C 端登录共享认证逻辑，但会额外校验管理员角色权限。</p>
     *
     * @param dto 登录请求 DTO（含账号和密码）
     * @return Sa-Token 令牌字符串
     * @throws com.payment.common.exception.BusinessException 认证失败或无管理员权限时抛出
     */
    String loginadmin(LoginDTO dto);

    /**
     * 根据用户名查询用户。
     *
     * @param username 用户名
     * @return 用户实体，不存在时返回 {@code null}
     */
    User getByUsername(String username);

    /**
     * 注册新用户。
     *
     * <p>校验用户名唯一性后创建用户，密码加密存储。</p>
     *
     * @param user 用户实体（含用户名、密码等基本信息）
     * @return 注册成功的用户信息
     * @throws com.payment.common.exception.BusinessException 用户名已被占用时抛出
     */
    User register(User user);

    /**
     * 微信小程序登录。
     *
     * <p>通过微信小程序 code 换取 openid，若用户首次登录则自动注册，
     * 返回用户信息和 Token。</p>
     *
     * @param dto 微信登录请求 DTO（含小程序 code）
     * @return 小程序用户 VO（含 Token 和用户信息）
     * @throws com.payment.common.exception.BusinessException 微信接口调用失败时抛出
     */
    MiniProgramUserVO wechatLogin(WechatLoginDTO dto);
}
