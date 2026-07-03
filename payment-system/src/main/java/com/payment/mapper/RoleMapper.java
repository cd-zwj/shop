package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色数据访问接口，提供角色表（sys_role）的 CRUD 操作。
 * 管理系统角色信息，支持按账号体系和用户ID查询关联角色。
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据账号体系和用户ID查询该用户关联的角色编码列表。
     */
    @Select("SELECT r.role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND ur.principal_type = #{principalType} AND r.status = 1")
    List<String> selectRoleCodesByPrincipal(@Param("userId") Long userId,
                                             @Param("principalType") String principalType);

    /**
     * 根据账号体系和用户ID查询该用户关联的完整角色对象列表。
     */
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND ur.principal_type = #{principalType} AND r.status = 1")
    List<Role> selectRolesByPrincipal(@Param("userId") Long userId,
                                       @Param("principalType") String principalType);
}
