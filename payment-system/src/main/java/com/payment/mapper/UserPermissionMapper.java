package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserPermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户权限关联数据访问接口，提供用户权限关联表（sys_user_permission）的 CRUD 操作。
 */
@Mapper
public interface UserPermissionMapper extends BaseMapper<UserPermission> {

    /**
     * 根据账号体系和用户ID查询该用户关联的所有权限ID列表。
     */
    @Select("SELECT permission_id FROM sys_user_permission " +
            "WHERE principal_type = #{principalType} AND user_id = #{userId}")
    List<Long> selectPermissionIdsByPrincipal(@Param("principalType") String principalType,
                                               @Param("userId") Long userId);

    /**
     * 根据账号体系和用户ID删除该用户的所有权限关联。
     */
    @Delete("DELETE FROM sys_user_permission WHERE principal_type = #{principalType} AND user_id = #{userId}")
    int deleteByPrincipal(@Param("principalType") String principalType,
                          @Param("userId") Long userId);

    /**
     * 根据账号体系、用户ID和权限ID删除指定的权限关联。
     */
    @Delete("DELETE FROM sys_user_permission " +
            "WHERE principal_type = #{principalType} AND user_id = #{userId} AND permission_id = #{permissionId}")
    int deleteByPrincipalAndPermissionId(@Param("principalType") String principalType,
                                         @Param("userId") Long userId,
                                         @Param("permissionId") Long permissionId);
}
