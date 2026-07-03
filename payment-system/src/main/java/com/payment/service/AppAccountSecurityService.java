package com.payment.service;

import com.payment.dto.AppAccountSecurityVO;
import com.payment.dto.AppChangePasswordDTO;

/**
 * 用户端账号安全服务接口。
 *
 * <p>面向 C 端用户提供账号安全状态查询和密码修改能力，
 * 承接 {@code V1AppAccountSecurityController} 的业务逻辑。</p>
 */
public interface AppAccountSecurityService {

    /**
     * 获取账号安全摘要信息（如是否已绑定手机号/邮箱、密码强度等）。
     *
     * @param platformUserId 平台用户ID
     * @return 账号安全摘要 VO
     */
    AppAccountSecurityVO getSecuritySummary(Long platformUserId);

    /**
     * 登录态内修改密码。
     *
     * <p>校验原密码正确后更新为新密码，修改成功后清除历史登录失败计数。</p>
     *
     * @param platformUserId 平台用户ID
     * @param dto            修改密码请求（含旧密码与新密码）
     * @throws com.payment.common.exception.BusinessException 旧密码不正确时抛出
     */
    void changePassword(Long platformUserId, AppChangePasswordDTO dto);
}
