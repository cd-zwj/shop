package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限视图对象（V1 AdminUser 接口）
 */
@Data
public class PermissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String permissionCode;
    private String permissionName;
    private String module;
    private String description;
    private LocalDateTime createTime;
}
