package com.payment.dto;

import lombok.Data;

import java.util.List;

/**
 * 商户端登录成功后的会话信息视图对象。
 */
@Data
public class V1MerchantSessionVO {

    /** 访问令牌（JWT） */
    private String token;

    /** 令牌过期时间（秒） */
    private long expiresIn;

    /** 平台用户ID */
    private Long platformUserId;

    /** 用户名 */
    private String username;

    /** 当前操作的租户（商户）ID */
    private Long tenantId;

    /** 当前操作的租户名称 */
    private String tenantName;

    /** 在当前租户中的员工角色 */
    private String employeeRole;

    /** 该用户关联的全部租户列表（支持多商户切换） */
    private List<V1MerchantTenantVO> tenants;
}
