package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.UserNotification;

/**
 * 用户通知服务接口。
 *
 * <p>面向 C 端用户提供通知的发送、查询、统计和已读状态管理能力，
 * 支持多种通知分类（订单、支付、退款、优惠券、系统、营销等）。
 * 承接 {@code V1AppNotificationController} 的业务逻辑。</p>
 */
public interface UserNotificationService {

    /**
     * 发送通知给指定用户（仅供内部服务调用，禁止暴露为 REST 接口）。
     *
     * @param platformUserId 目标用户ID
     * @param title          通知标题（非空，不超过200字，自动去除 HTML）
     * @param content        通知内容（非空，不超过5000字，自动去除 HTML）
     * @param category       通知分类（ORDER / PAYMENT / REFUND / COUPON / SYSTEM / PROMOTION）
     * @return 创建的通知记录
     * @throws com.payment.common.exception.BusinessException 参数不合法时抛出
     */
    UserNotification send(Long platformUserId, String title, String content, String category);

    /**
     * 分页查询当前用户的通知列表。
     *
     * @param platformUserId 平台用户ID
     * @param current        当前页码
     * @param size           每页条数
     * @return 通知分页结果（按创建时间倒序）
     */
    Page<UserNotification> list(Long platformUserId, Integer current, Integer size);

    /**
     * 统计当前用户未读通知数量。
     *
     * @param platformUserId 平台用户ID
     * @return 未读通知数量
     */
    long countUnread(Long platformUserId);

    /**
     * 标记单条通知为已读。
     *
     * @param platformUserId 平台用户ID
     * @param notificationId 通知ID
     * @return 更新后的通知记录
     * @throws com.payment.common.exception.BusinessException 通知不存在或不属于当前用户时抛出
     */
    UserNotification markRead(Long platformUserId, Long notificationId);

    /**
     * 标记当前用户全部未读通知为已读。
     *
     * @param platformUserId 平台用户ID
     * @return 受影响的通知条数
     */
    int markAllRead(Long platformUserId);
}
