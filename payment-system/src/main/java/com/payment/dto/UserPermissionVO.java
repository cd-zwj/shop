package com.payment.dto;

import lombok.Data;
import java.util.List;

/**
 * 用户权限详情视图对象，用于返回用户的角色权限、额外权限及汇总权限列表。
 */
@Data
public class UserPermissionVO {
    /** 用户 ID */
    private Long userId;
    /** 用户名 */
    private String username;
    /** 角色拥有的权限编码列表 */
    private List<String> rolePermissions;
    /** 用户额外分配的权限编码列表 */
    private List<String> extraPermissions;
    /** 所有权限编码汇总（角色权限 + 额外权限去重合并） */
    private List<String> allPermissions;
}
