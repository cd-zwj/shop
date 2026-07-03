package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户数据访问接口，提供用户表（sys_user）的 CRUD 操作。
 *
 * @deprecated sys_user 仅保留历史兼容；新身份认证请使用 platform_user。
 */
@Mapper
@Deprecated
public interface UserMapper extends BaseMapper<User> {
}
