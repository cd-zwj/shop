package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户通知数据访问接口，用于执行通知数据的增删改查。
 */
@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
}
