package com.payment.service.delivery;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.OrderDeliveryRecord;

import java.util.List;

/**
 * 订单交付服务。
 *
 * 流程入口：
 * <ol>
 *   <li>{@link #enqueueDelivery(String)} — 支付成功后投递 V1_ORDER_DELIVERY_QUEUE</li>
 *   <li>{@link #deliverOrder(String)} — Consumer 收到消息后调用，按 item 分发到策略</li>
 * </ol>
 *
 * 用户/商户操作：
 * <ul>
 *   <li>{@link #listUserDeliveries} — C 端"我的已购"</li>
 *   <li>{@link #markShipped} — 实物商家发货</li>
 *   <li>{@link #confirmReceived} — 用户确认收货 / 已使用</li>
 *   <li>{@link #revoke} — 退款回收</li>
 * </ul>
 */
public interface OrderDeliveryService {

    /** 支付成功后把交付事件写入 Outbox，由 MessageOutboxRetryScheduler 投递到 V1_ORDER_DELIVERY_QUEUE。 */
    void enqueueDelivery(String orderNo);

    /** Consumer 入口：对订单所有 item 调对应策略，落 order_delivery_record，更新 item.delivery_status。 */
    void deliverOrder(String orderNo);

    /** C 端：列出当前用户的交付记录，status 可空表示全部。 */
    Page<OrderDeliveryRecord> listUserDeliveries(Long platformUserId, String status, Integer current, Integer size);

    /** C 端：查看单条交付详情（含 payload）。 */
    OrderDeliveryRecord getUserDelivery(Long platformUserId, Long recordId);

    /** C 端：用户确认收货 / 已使用，状态 → CONFIRMED。 */
    OrderDeliveryRecord confirmReceived(Long platformUserId, Long recordId);

    /** B 端：实物商品发货，payload 写入物流单号，状态 → DELIVERED。 */
    OrderDeliveryRecord markShipped(Long tenantId, Long orderItemId, String shippingNo, String logisticsCompany);

    /** B 端：服务商品核销，校验核销码后状态 → CONFIRMED。 */
    OrderDeliveryRecord verifyService(Long tenantId, String verifyCode);

    /** 退款链路调用：找到对应订单项的交付记录调 strategy.revoke 并标记 REVOKED。 */
    List<OrderDeliveryRecord> revokeByOrderItem(Long orderItemId);

    /** 退款链路调用：整单退款时按订单号批量回收。 */
    List<OrderDeliveryRecord> revokeByOrderNo(String orderNo);
}
