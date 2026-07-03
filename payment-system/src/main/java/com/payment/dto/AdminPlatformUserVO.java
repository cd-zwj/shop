package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台管理端用户视图对象，展示平台注册用户的基本信息与关联统计。
 */
@Data
public class AdminPlatformUserVO {

    /** 用户ID */
    private Long id;

    /** 用户编号 */
    private String userNo;

    /** 用户名 */
    private String username;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 账号状态（0-禁用，1-启用） */
    private Integer status;

    /** 注册时间 */
    private LocalDateTime createTime;

    /** 该用户作为会员关联的租户数 */
    private Long memberTenantCount;

    /** 该用户作为员工关联的租户数 */
    private Long employeeTenantCount;

    /** 统一钱包余额 */
    private BigDecimal unifiedWalletBalance;
}
