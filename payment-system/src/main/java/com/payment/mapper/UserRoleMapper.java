package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户角色关联数据访问接口，提供用户角色关联表（sys_user_role）的 CRUD 操作。
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 给指定账号体系下的用户分配角色。
     */
    @Insert("INSERT INTO sys_user_role (principal_type, user_id, role_id) " +
            "VALUES (#{principalType}, #{userId}, #{roleId})")
    int insertUserRole(@Param("principalType") String principalType,
                       @Param("userId") Long userId,
                       @Param("roleId") Long roleId);

    /**
     * 删除指定账号体系下用户的所有角色关联。
     */
    @Delete("DELETE FROM sys_user_role WHERE principal_type = #{principalType} AND user_id = #{userId}")
    int deleteByPrincipal(@Param("principalType") String principalType,
                          @Param("userId") Long userId);
}
