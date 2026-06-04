package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.UserNotification;

/**
 * 用户通知服务接口，用于定义通知列表和已读状态能力。
 */
public interface UserNotificationService {

    /**
     * 查询当前用户通知列表。
     */
    Page<UserNotification> list(Long platformUserId, Integer current, Integer size);

    /**
     * 标记当前用户通知为已读。
     */
    UserNotification markRead(Long platformUserId, Long notificationId);
}
