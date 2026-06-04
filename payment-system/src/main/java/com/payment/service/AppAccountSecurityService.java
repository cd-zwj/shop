package com.payment.service;

import com.payment.dto.AppAccountSecurityVO;
import com.payment.dto.AppChangePasswordDTO;

/**
 * 用户端账号安全服务接口，用于定义安全状态和密码修改能力。
 */
public interface AppAccountSecurityService {

    /**
     * 获取账号安全摘要。
     */
    AppAccountSecurityVO getSecuritySummary(Long platformUserId);

    /**
     * 登录态内修改密码。
     */
    void changePassword(Long platformUserId, AppChangePasswordDTO dto);
}
