package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户登录日志数据访问接口，提供用户登录日志表（user_login_log）的 CRUD 操作。
 * 记录用户登录时间、IP 地址、设备信息等，用于安全审计和异常登录检测。
 */
@Mapper
public interface UserLoginLogMapper extends BaseMapper<UserLoginLog> {
}
