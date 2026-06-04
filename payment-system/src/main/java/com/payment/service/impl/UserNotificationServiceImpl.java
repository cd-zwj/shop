package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.entity.UserNotification;
import com.payment.mapper.UserNotificationMapper;
import com.payment.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户通知服务实现类，用于提供只读列表和已读状态能力。
 */
@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

    private final UserNotificationMapper notificationMapper;

    /**
     * 查询通知列表。
     */
    @Override
    public Page<UserNotification> list(Long platformUserId, Integer current, Integer size) {
        int pageNo = current == null || current < 1 ? 1 : current;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 50);
        return notificationMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getPlatformUserId, platformUserId)
                .eq(UserNotification::getDeleted, 0)
                .orderByAsc(UserNotification::getReadStatus)
                .orderByDesc(UserNotification::getCreateTime)
                .orderByDesc(UserNotification::getId));
    }

    /**
     * 标记已读。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserNotification markRead(Long platformUserId, Long notificationId) {
        UserNotification notification = notificationMapper.selectById(notificationId);
        if (notification == null
                || !platformUserId.equals(notification.getPlatformUserId())
                || Integer.valueOf(1).equals(notification.getDeleted())) {
            throw new BusinessException("通知不存在");
        }

        if (Integer.valueOf(1).equals(notification.getReadStatus())) {
            return notification;
        }

        notification.setReadStatus(1);
        notification.setReadTime(LocalDateTime.now());
        notificationMapper.updateById(notification);
        return notification;
    }
}
