package com.payment.dto;

import lombok.Data;

/**
 * 商户端租户简要信息视图对象，用于商户登录后展示可切换的租户列表。
 */
@Data
public class V1MerchantTenantVO {

    /** 租户 ID */
    private Long tenantId;

    /** 租户名称 */
    private String tenantName;

    /** 当前用户在该租户中的员工角色 */
    private String employeeRole;
}
