package com.payment.service.delivery;

import com.payment.entity.OrderDeliveryRecord;

import java.util.List;

/**
 * 订单交付服务。
 *
 * 流程入口：
 * <ol>
 *   <li>{@link #enqueueDelivery(String)} — 支付成功后投递 V1_ORDER_DELIVERY_QUEUE</li>
 *   <li>{@link #deliverOrder(String)} — Consumer 收到消息后创建自提交付记录</li>
 * </ol>
 *
 * 商户操作：
 * <ul>
 *   <li>{@link #verifyPickup} — 店员核销自提码</li>
 * </ul>
 */
public interface OrderDeliveryService {

    /** 支付成功后把交付事件写入 Outbox，由 MessageOutboxRetryScheduler 投递到 V1_ORDER_DELIVERY_QUEUE。 */
    void enqueueDelivery(String orderNo);

    /** Consumer 入口：对订单所有 item 创建自提交付记录并更新 item.delivery_status。 */
    void deliverOrder(String orderNo);

    /** B 端：到店自提核销，校验指定门店的自提码并写入领取留痕；不改变订单完成状态。 */
    OrderDeliveryRecord verifyPickup(Long tenantId, Long storeId, String pickupCode, Long operatorId);

    /** 退款链路调用：找到对应订单项的交付记录并标记 REVOKED。 */
    List<OrderDeliveryRecord> revokeByOrderItem(Long orderItemId);

    /** 退款链路调用：整单退款时按订单号批量回收。 */
    List<OrderDeliveryRecord> revokeByOrderNo(String orderNo);
}
