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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户通知服务实现类，提供通知发送、查询和已读状态能力。
 */
@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

    private final UserNotificationMapper notificationMapper;

    /** 允许的通知分类 */
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "ORDER", "PAYMENT", "REFUND", "COUPON", "SYSTEM", "PROMOTION"
    );

    /**
     * 发送通知给指定用户（仅供内部服务调用，禁止暴露为 REST 接口）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserNotification send(Long platformUserId, String title, String content, String category) {
        // --- 输入验证 ---
        if (platformUserId == null || platformUserId <= 0) {
            throw new BusinessException("通知目标用户ID不合法");
        }
        if (!StringUtils.hasText(category) || !ALLOWED_CATEGORIES.contains(category)) {
            throw new BusinessException("通知分类不合法，允许值: " + ALLOWED_CATEGORIES);
        }

        // --- XSS 防御：先清洗，再校验（防止 "<br>" 通过非空校验后变空字符串） ---
        String safeTitle = stripHtml(title);
        String safeContent = stripHtml(content);

        if (!StringUtils.hasText(safeTitle) || safeTitle.length() > 200) {
            throw new BusinessException("通知标题不能为空且不超过200字");
        }
        if (!StringUtils.hasText(safeContent) || safeContent.length() > 5000) {
            throw new BusinessException("通知内容不能为空且不超过5000字");
        }

        LocalDateTime now = LocalDateTime.now();
        UserNotification notification = new UserNotification();
        notification.setPlatformUserId(platformUserId);
        notification.setTitle(safeTitle);
        notification.setContent(safeContent);
        notification.setCategory(category);
        notification.setReadStatus(0);
        notification.setDeleted(0);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);
        notificationMapper.insert(notification);
        return notification;
    }

    /**
     * 去除 HTML 标签，仅保留纯文本。
     */
    private String stripHtml(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("<[^>]*>", "");
    }

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
