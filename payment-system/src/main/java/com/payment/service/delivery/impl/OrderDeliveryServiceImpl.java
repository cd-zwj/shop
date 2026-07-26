package com.payment.service.delivery.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.payment.common.BusinessException;
import com.payment.config.RabbitMQConfig;
import com.payment.entity.MessageOutbox;
import com.payment.entity.OrderDeliveryRecord;
import com.payment.entity.OrderFulfillmentAction;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.DeliveryStatusEnum;
import com.payment.enums.OrderStatusEnum;
import com.payment.mapper.OrderFulfillmentActionMapper;
import com.payment.mapper.OrderDeliveryRecordMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.OutboxPublisher;
import com.payment.service.UserNotificationService;
import com.payment.service.delivery.OrderDeliveryService;
import com.payment.service.outbox.OutboxMessageCommand;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 到店自提凭证服务。支付成功后为每个订单项创建取货凭证；不承担物流或虚拟内容交付。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDeliveryServiceImpl implements OrderDeliveryService {

    private static final SecureRandom PICKUP_CODE_RANDOM = new SecureRandom();

    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final OrderDeliveryRecordMapper deliveryRecordMapper;
    private final OrderFulfillmentActionMapper fulfillmentActionMapper;
    private final OutboxPublisher outboxPublisher;
    private final UserNotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enqueueDelivery(String orderNo) {
        MessageOutbox outbox = outboxPublisher.publish(OutboxMessageCommand.builder()
                .messagePrefix("DLV")
                .bizType("ORDER_DELIVERY")
                .bizNo(orderNo)
                .routingKey(RabbitMQConfig.V1_ORDER_DELIVERY_QUEUE)
                .messageBody(Map.of("bizNo", orderNo, "bizType", "ORDER_DELIVERY"))
                .build());
        log.info("Pickup certificate enqueued, orderNo={}, outboxId={}", orderNo, outbox.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverOrder(String orderNo) {
        SalesOrder order = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderNo, orderNo)
                .eq(SalesOrder::getDeleted, 0));
        if (order == null) {
            log.warn("订单不存在，跳过生成自提凭证 orderNo={}", orderNo);
            return;
        }
        if (!"STORE_PICKUP".equals(order.getFulfillmentMode()) || order.getStoreId() == null) {
            throw new BusinessException("当前订单不是有效的到店自提订单");
        }

        for (SalesOrderItem item : salesOrderItemMapper.selectByOrderId(order.getId())) {
            createPickupCertificate(order, item);
        }
    }

    private void createPickupCertificate(SalesOrder order, SalesOrderItem item) {
        OrderDeliveryRecord existing = deliveryRecordMapper.selectOne(new LambdaQueryWrapper<OrderDeliveryRecord>()
                .eq(OrderDeliveryRecord::getOrderItemId, item.getId())
                .eq(OrderDeliveryRecord::getDeleted, 0));
        if (existing != null) {
            return;
        }

        OrderDeliveryRecord record = new OrderDeliveryRecord();
        record.setTenantId(order.getTenantId());
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setOrderItemId(item.getId());
        record.setPlatformUserId(order.getPlatformUserId());
        record.setProductId(item.getProductId());
        record.setProductName(item.getProductName());
        record.setStatus(DeliveryStatusEnum.DELIVERED.name());
        record.setPayload(JsonUtils.toJson(Map.of("pickupCode", nextPickupCode(), "storeId", order.getStoreId())));
        record.setRetryCount(0);
        record.setDeliveredTime(LocalDateTime.now());
        try {
            deliveryRecordMapper.insert(record);
        } catch (DuplicateKeyException ignored) {
            return;
        }

        salesOrderItemMapper.update(null, new LambdaUpdateWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getId, item.getId())
                .set(SalesOrderItem::getDeliveryStatus, DeliveryStatusEnum.DELIVERED.name())
                .set(SalesOrderItem::getDeliveredTime, record.getDeliveredTime()));
        notifyPickupCode(order, item);
    }

    private String nextPickupCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = String.format("%08d", PICKUP_CODE_RANDOM.nextInt(100_000_000));
            Long count = deliveryRecordMapper.selectCount(new LambdaQueryWrapper<OrderDeliveryRecord>()
                    .eq(OrderDeliveryRecord::getDeleted, 0)
                    .apply("JSON_UNQUOTE(JSON_EXTRACT(payload, '$.pickupCode')) = {0}", code));
            if (count == null || count == 0) {
                return code;
            }
        }
        throw new BusinessException("取货码生成失败，请重试");
    }

    private void notifyPickupCode(SalesOrder order, SalesOrderItem item) {
        try {
            notificationService.send(order.getPlatformUserId(), "取货码已生成",
                    "订单 " + order.getOrderNo() + " 的商品「" + item.getProductName() + "」已生成取货码，请等待商家备货。", "ORDER");
        } catch (Exception exception) {
            log.warn("发送取货凭证通知失败 orderNo={}, itemId={}", order.getOrderNo(), item.getId(), exception);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDeliveryRecord verifyPickup(Long tenantId, Long storeId, String pickupCode, Long operatorId) {
        if (storeId == null || storeId <= 0) throw new BusinessException("自提门店不能为空");
        if (pickupCode == null || !pickupCode.trim().matches("\\d{8}")) throw new BusinessException("取货码必须为 8 位数字");
        if (operatorId == null || operatorId <= 0) throw new BusinessException("核销操作人不能为空");

        String code = pickupCode.trim();
        OrderDeliveryRecord record = deliveryRecordMapper.selectOne(new LambdaQueryWrapper<OrderDeliveryRecord>()
                .eq(OrderDeliveryRecord::getTenantId, tenantId)
                .eq(OrderDeliveryRecord::getDeleted, 0)
                .apply("JSON_UNQUOTE(JSON_EXTRACT(payload, '$.pickupCode')) = {0}", code)
                .apply("JSON_UNQUOTE(JSON_EXTRACT(payload, '$.storeId')) = {0}", String.valueOf(storeId)));
        if (record == null) throw new BusinessException("取货码不存在或不属于当前门店");
        if (DeliveryStatusEnum.CONFIRMED.name().equals(record.getStatus())) return record;
        if (!DeliveryStatusEnum.DELIVERED.name().equals(record.getStatus())) throw new BusinessException("当前取货码不可核销");

        SalesOrder order = salesOrderMapper.selectById(record.getOrderId());
        if (order == null || !OrderStatusEnum.COMPLETED.name().equals(order.getOrderStatus())) {
            throw new BusinessException("订单尚未确认备货完成，暂不能核销");
        }

        record.setStatus(DeliveryStatusEnum.CONFIRMED.name());
        record.setConfirmedTime(LocalDateTime.now());
        deliveryRecordMapper.updateById(record);
        salesOrderItemMapper.update(null, new LambdaUpdateWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getId, record.getOrderItemId())
                .set(SalesOrderItem::getDeliveryStatus, DeliveryStatusEnum.CONFIRMED.name()));
        OrderFulfillmentAction action = new OrderFulfillmentAction();
        action.setTenantId(record.getTenantId());
        action.setStoreId(storeId);
        action.setOrderId(record.getOrderId());
        action.setOrderNo(record.getOrderNo());
        action.setAction("PICKUP_VERIFIED");
        action.setFromStatus(order.getOrderStatus());
        action.setToStatus(order.getOrderStatus());
        action.setOperatorId(operatorId);
        action.setRemark("到店自提核销");
        fulfillmentActionMapper.insert(action);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OrderDeliveryRecord> revokeByOrderItem(Long orderItemId) {
        return revoke(deliveryRecordMapper.selectList(new LambdaQueryWrapper<OrderDeliveryRecord>()
                .eq(OrderDeliveryRecord::getOrderItemId, orderItemId)
                .eq(OrderDeliveryRecord::getDeleted, 0)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OrderDeliveryRecord> revokeByOrderNo(String orderNo) {
        return revoke(deliveryRecordMapper.selectList(new LambdaQueryWrapper<OrderDeliveryRecord>()
                .eq(OrderDeliveryRecord::getOrderNo, orderNo)
                .eq(OrderDeliveryRecord::getDeleted, 0)));
    }

    private List<OrderDeliveryRecord> revoke(List<OrderDeliveryRecord> records) {
        List<OrderDeliveryRecord> changed = new ArrayList<>();
        for (OrderDeliveryRecord record : records) {
            if (DeliveryStatusEnum.REVOKED.name().equals(record.getStatus())) continue;
            record.setStatus(DeliveryStatusEnum.REVOKED.name());
            record.setRevokedTime(LocalDateTime.now());
            deliveryRecordMapper.updateById(record);
            salesOrderItemMapper.update(null, new LambdaUpdateWrapper<SalesOrderItem>()
                    .eq(SalesOrderItem::getId, record.getOrderItemId())
                    .set(SalesOrderItem::getDeliveryStatus, DeliveryStatusEnum.REVOKED.name()));
            changed.add(record);
        }
        return changed;
    }
}
