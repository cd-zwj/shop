package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserPermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserPermissionMapper extends BaseMapper<UserPermission> {

    @Select("SELECT permission_id FROM sys_user_permission WHERE user_id = #{userId}")
    List<Long> selectPermissionIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_permission WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_permission WHERE user_id = #{userId} AND permission_id = #{permissionId}")
    int deleteByUserIdAndPermissionId(@Param("userId") Long userId, @Param("permissionId") Long permissionId);
}
