package com.payment.dto;

import lombok.Data;
import java.util.List;

/**
 * 用户权限分配数据传输对象，用于批量设置用户的额外权限。
 */
@Data
public class UserPermissionDTO {
    /** 权限 ID 列表 */
    private List<Long> permissionIds;
}
