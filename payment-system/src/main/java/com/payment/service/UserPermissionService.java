package com.payment.service;

import com.payment.dto.UserPermissionVO;
import java.util.List;

/**
 * 用户权限管理服务接口。
 *
 * <p>面向平台管理员提供用户权限的查询、授予、撤销和批量设置能力，
 * 基于 RBAC 五表权限模型（用户-角色-权限）实现细粒度访问控制。</p>
 */
public interface UserPermissionService {

    /**
     * 获取用户的权限详情（含角色列表和权限码集合）。
     *
     * @param userId 用户ID
     * @return 用户权限详情 VO
     */
    UserPermissionVO getUserPermissions(Long userId);

    /**
     * 授予用户单个权限。
     *
     * @param userId       用户ID
     * @param permissionId 权限ID
     * @throws com.payment.common.exception.BusinessException 用户或权限不存在，或已拥有该权限时抛出
     */
    void grantPermission(Long userId, Long permissionId);

    /**
     * 撤销用户单个权限。
     *
     * @param userId       用户ID
     * @param permissionId 权限ID
     * @throws com.payment.common.exception.BusinessException 用户或权限不存在，或未拥有该权限时抛出
     */
    void revokePermission(Long userId, Long permissionId);

    /**
     * 批量设置用户权限（全量覆盖）。
     *
     * <p>清除用户现有权限后重新设置，用于权限管理页面的批量操作。</p>
     *
     * @param userId        用户ID
     * @param permissionIds 目标权限ID列表
     */
    void setUserPermissions(Long userId, List<Long> permissionIds);
}
