package com.payment.service;

/**
 * 登录安全服务接口
 */
public interface LoginSecurityService {

    /**
     * 检查账号是否被锁定，锁定则抛出异常
     * @param account 登录账号
     */
    void checkNotLocked(String account);

    /**
     * 记录一次登录失败，达到阈值则自动锁定
     * @param account 登录账号
     * @param ip 登录IP
     */
    void recordFailure(String account, String ip);

    /**
     * 登录成功后清除失败记录
     * @param account 登录账号
     */
    void clearFailures(String account);
}
