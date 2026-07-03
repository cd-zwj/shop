package com.payment.service;

/**
 * 登录安全服务接口。
 *
 * <p>负责登录失败次数追踪与账号锁定机制，
 * 配合 {@link AuthCaptchaService} 共同防范暴力破解攻击。
 * 失败计数和锁定状态存储于 Redis，账号锁定后需等待冷却时间自动解锁。</p>
 */
public interface LoginSecurityService {

    /**
     * 检查账号是否被锁定。
     *
     * <p>若账号因连续登录失败已被锁定，直接抛出异常阻止登录。</p>
     *
     * @param account 登录账号（手机号/邮箱/用户名）
     * @throws com.payment.common.exception.BusinessException 账号已被锁定时抛出，提示剩余锁定时间
     */
    void checkNotLocked(String account);

    /**
     * 记录一次登录失败。
     *
     * <p>累加失败次数，当连续失败次数达到系统阈值时自动锁定账号。</p>
     *
     * @param account 登录账号
     * @param ip      登录 IP 地址，用于关联风控分析
     */
    void recordFailure(String account, String ip);

    /**
     * 清除登录失败记录。
     *
     * <p>登录成功后调用，重置失败计数器。</p>
     *
     * @param account 登录账号
     */
    void clearFailures(String account);
}
