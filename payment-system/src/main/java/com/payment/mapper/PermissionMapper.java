package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限Mapper
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据用户ID查询权限编码列表
     */
    @Select("SELECT DISTINCT p.permission_code FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询权限编码列表
     */
    @Select("SELECT p.permission_code FROM sys_permission p " +
            "INNER JOIN sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId}")
    List<String> selectPermissionCodesByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询用户额外权限编码列表
     */
    @Select("SELECT DISTINCT p.permission_code FROM sys_permission p " +
            "INNER JOIN sys_user_permission up ON p.id = up.permission_id " +
            "WHERE up.user_id = #{userId}")
    List<String> selectExtraPermissionCodesByUserId(@Param("userId") Long userId);
}
