package com.payment.service;

import com.payment.dto.UserPermissionVO;
import java.util.List;

public interface UserPermissionService {

    /**
     * 获取用户权限详情
     */
    UserPermissionVO getUserPermissions(Long userId);

    /**
     * 授予用户权限
     */
    void grantPermission(Long userId, Long permissionId);

    /**
     * 撤销用户权限
     */
    void revokePermission(Long userId, Long permissionId);

    /**
     * 批量设置用户权限
     */
    void setUserPermissions(Long userId, List<Long> permissionIds);
}
