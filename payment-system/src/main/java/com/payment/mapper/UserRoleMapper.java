package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户角色关联Mapper
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 给用户分配角色
     */
    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 删除用户的所有角色
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
