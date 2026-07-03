package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限数据访问接口，提供权限表（sys_permission）的 CRUD 操作。
 * 管理系统权限信息，支持按账号体系和用户ID查询关联权限编码。
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据账号体系和用户ID查询该用户通过角色间接关联的所有权限编码列表（去重）。
     */
    @Select("SELECT DISTINCT p.permission_code FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND ur.principal_type = #{principalType}")
    List<String> selectPermissionCodesByPrincipal(@Param("userId") Long userId,
                                                   @Param("principalType") String principalType);

    /**
     * 根据角色ID查询该角色关联的所有权限编码列表。
     */
    @Select("SELECT p.permission_code FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId}")
    List<String> selectPermissionCodesByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据账号体系和用户ID查询用户额外直分配的权限编码列表（去重）。
     */
    @Select("SELECT DISTINCT p.permission_code FROM sys_permission p " +
            "INNER JOIN sys_user_permission up ON p.id = up.permission_id " +
            "WHERE up.user_id = #{userId} AND up.principal_type = #{principalType}")
    List<String> selectExtraPermissionCodesByPrincipal(@Param("userId") Long userId,
                                                        @Param("principalType") String principalType);
}
