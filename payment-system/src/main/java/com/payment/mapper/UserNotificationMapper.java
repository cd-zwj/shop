package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户通知数据访问接口，提供用户通知表（user_notification）的 CRUD 操作。
 * 管理系统通知、订单消息、营销推送等各类用户通知。
 */
@Mapper
public interface UserNotificationMapper extends BaseMapper<UserNotification> {
}
