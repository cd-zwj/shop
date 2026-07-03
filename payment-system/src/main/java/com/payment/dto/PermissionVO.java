package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限视图对象，用于返回系统权限的详细信息（V1 AdminUser 接口）。
 */
@Data
public class PermissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 权限 ID */
    private Long id;
    /** 权限编码（如 user:create, order:export） */
    private String permissionCode;
    /** 权限名称（如 创建用户、导出订单） */
    private String permissionName;
    /** 所属模块（如 用户管理、订单管理） */
    private String module;
    /** 权限描述 */
    private String description;
    /** 创建时间 */
    private LocalDateTime createTime;
}
