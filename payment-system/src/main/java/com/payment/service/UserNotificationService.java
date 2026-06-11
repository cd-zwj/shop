package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.UserNotification;

/**
 * 用户通知服务接口，用于定义通知的发送、查询和已读状态能力。
 */
public interface UserNotificationService {

    /**
     * 发送通知给指定用户（仅供内部服务调用，禁止暴露为 REST 接口）。
     *
     * @param platformUserId 目标用户ID
     * @param title          通知标题（非空，≤200字，自动去除 HTML）
     * @param content        通知内容（非空，≤5000字，自动去除 HTML）
     * @param category       通知分类（ORDER / PAYMENT / REFUND / COUPON / SYSTEM / PROMOTION）
     * @return 创建的通知记录
     * @throws BusinessException 参数不合法时抛出
     */
    UserNotification send(Long platformUserId, String title, String content, String category);

    /**
     * 查询当前用户通知列表。
     */
    Page<UserNotification> list(Long platformUserId, Integer current, Integer size);

    /**
     * 标记当前用户通知为已读。
     */
    UserNotification markRead(Long platformUserId, Long notificationId);
}
