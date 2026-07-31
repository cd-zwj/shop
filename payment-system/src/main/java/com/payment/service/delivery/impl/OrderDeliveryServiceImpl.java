package com.payment.service.delivery.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
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
import com.payment.service.AuditLogService;
import com.payment.service.OutboxPublisher;
import com.payment.service.UserNotificationService;
import com.payment.service.MerchantStoreScope;
import com.payment.service.impl.MerchantStoreScopeService;
import com.payment.service.delivery.OrderDeliveryService;
import com.payment.service.delivery.PickupCodePayloadService;
import com.payment.service.outbox.OutboxMessageCommand;
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
    private static final int PICKUP_CODE_MAX_ATTEMPTS = 20;

    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final OrderDeliveryRecordMapper deliveryRecordMapper;
    private final OrderFulfillmentActionMapper fulfillmentActionMapper;
    private final OutboxPublisher outboxPublisher;
    private final UserNotificationService notificationService;
    private final AuditLogService auditLogService;
    private final MerchantStoreScopeService merchantStoreScopeService;
    private final PickupCodePayloadService pickupCodePayloadService;

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
        record.setStoreId(order.getStoreId());
        record.setRetryCount(0);
        record.setDeliveredTime(LocalDateTime.now());

        // 取货码唯一性由 uk_tenant_pickup_hash 唯一约束兜底：
        // 插入冲突时若订单项凭证已存在则幂等返回，否则视为哈希撞码换码重试。
        for (int attempt = 0; attempt < PICKUP_CODE_MAX_ATTEMPTS; attempt++) {
            String code = String.format("%08d", PICKUP_CODE_RANDOM.nextInt(100_000_000));
            record.setId(null);
            record.setPayload(pickupCodePayloadService.createEncryptedPayload(
                    order.getTenantId(), order.getOrderNo(), item.getId(), order.getStoreId(), code));
            record.setPickupCodeHash(DigestUtil.sha256Hex(code));
            try {
                deliveryRecordMapper.insert(record);
            } catch (DuplicateKeyException e) {
                Long itemCount = deliveryRecordMapper.selectCount(new LambdaQueryWrapper<OrderDeliveryRecord>()
                        .eq(OrderDeliveryRecord::getOrderItemId, item.getId())
                        .eq(OrderDeliveryRecord::getDeleted, 0));
                if (itemCount != null && itemCount > 0) {
                    return;
                }
                log.info("取货码哈希冲突，重新生成 orderNo={}, attempt={}", order.getOrderNo(), attempt + 1);
                continue;
            }

            salesOrderItemMapper.update(null, new LambdaUpdateWrapper<SalesOrderItem>()
                    .eq(SalesOrderItem::getId, item.getId())
                    .set(SalesOrderItem::getDeliveryStatus, DeliveryStatusEnum.DELIVERED.name())
                    .set(SalesOrderItem::getDeliveredTime, record.getDeliveredTime()));
            notifyPickupCode(order, item);
            return;
        }
        throw new BusinessException("取货码生成失败，请重试");
    }

    @Override
    public Map<Long, String> getPickupCodesForUser(Long tenantId, Long platformUserId, String orderNo) {
        if (tenantId == null || tenantId <= 0 || platformUserId == null || platformUserId <= 0
                || orderNo == null || orderNo.isBlank()) {
            throw new BusinessException("取货码查询参数不合法");
        }
        List<OrderDeliveryRecord> records = deliveryRecordMapper.selectList(
                new LambdaQueryWrapper<OrderDeliveryRecord>()
                        .eq(OrderDeliveryRecord::getTenantId, tenantId)
                        .eq(OrderDeliveryRecord::getPlatformUserId, platformUserId)
                        .eq(OrderDeliveryRecord::getOrderNo, orderNo)
                        .in(OrderDeliveryRecord::getStatus,
                                DeliveryStatusEnum.DELIVERED.name(), DeliveryStatusEnum.CONFIRMED.name())
                        .eq(OrderDeliveryRecord::getDeleted, 0));
        Map<Long, String> result = new java.util.LinkedHashMap<>();
        for (OrderDeliveryRecord record : records) {
            if (record.getOrderItemId() != null) {
                result.put(record.getOrderItemId(), pickupCodePayloadService.readPickupCode(record));
            }
        }
        return Map.copyOf(result);
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

        MerchantStoreScope scope = merchantStoreScopeService.resolve(
                tenantId, operatorId, MerchantPermission.ORDER_MANAGE);
        try {
            merchantStoreScopeService.requireStoreAccess(scope, storeId);
        } catch (BusinessException exception) {
            auditPickupFailure(tenantId, storeId, operatorId, "操作人无门店权限");
            throw exception;
        }

        String code = pickupCode.trim();
        // 按哈希走 uk_tenant_pickup_hash 唯一索引查询；门店归属一并强校验。
        OrderDeliveryRecord record = deliveryRecordMapper.selectOne(new LambdaQueryWrapper<OrderDeliveryRecord>()
                .eq(OrderDeliveryRecord::getTenantId, tenantId)
                .eq(OrderDeliveryRecord::getPickupCodeHash, DigestUtil.sha256Hex(code))
                .eq(OrderDeliveryRecord::getDeleted, 0));
        if (record == null || !storeId.equals(record.getStoreId())) {
            auditPickupFailure(tenantId, storeId, operatorId, "取货码不存在或不属于当前门店");
            throw new BusinessException("取货码不存在或不属于当前门店");
        }

        SalesOrder order = salesOrderMapper.selectById(record.getOrderId());
        if (order == null || !tenantId.equals(order.getTenantId()) || !storeId.equals(order.getStoreId())) {
            auditPickupFailure(tenantId, storeId, operatorId, "取货凭证与订单归属不一致");
            throw new BusinessException("取货码不存在或不属于当前门店");
        }
        if (DeliveryStatusEnum.CONFIRMED.name().equals(record.getStatus())) return record;
        if (!DeliveryStatusEnum.DELIVERED.name().equals(record.getStatus())) {
            auditPickupFailure(tenantId, storeId, operatorId, "取货码状态不可核销: " + record.getStatus());
            throw new BusinessException("当前取货码不可核销");
        }
        if (!OrderStatusEnum.COMPLETED.name().equals(order.getOrderStatus())) {
            auditPickupFailure(tenantId, storeId, operatorId, "订单未完成备货即尝试核销, orderNo=" + record.getOrderNo());
            throw new BusinessException("订单尚未确认备货完成，暂不能核销");
        }

        // 条件更新防并发双核销：仅 DELIVERED -> CONFIRMED 允许成功。
        LocalDateTime confirmedTime = LocalDateTime.now();
        int updated = deliveryRecordMapper.update(null, new LambdaUpdateWrapper<OrderDeliveryRecord>()
                .eq(OrderDeliveryRecord::getId, record.getId())
                .eq(OrderDeliveryRecord::getStatus, DeliveryStatusEnum.DELIVERED.name())
                .set(OrderDeliveryRecord::getStatus, DeliveryStatusEnum.CONFIRMED.name())
                .set(OrderDeliveryRecord::getConfirmedTime, confirmedTime)
                .set(OrderDeliveryRecord::getVerifiedBy, operatorId));
        if (updated == 0) {
            // 并发场景：另一请求已完成核销，幂等返回最新记录。
            return deliveryRecordMapper.selectById(record.getId());
        }
        record.setStatus(DeliveryStatusEnum.CONFIRMED.name());
        record.setConfirmedTime(confirmedTime);
        record.setVerifiedBy(operatorId);
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

    /**
     * 核销失败留痕：写入审计日志供安全追溯（错误码穷举、跨店尝试等），
     * 审计写入失败不阻断核销主流程的异常返回。
     */
    private void auditPickupFailure(Long tenantId, Long storeId, Long operatorId, String reason) {
        try {
            auditLogService.log(tenantId, operatorId, "MERCHANT", null,
                    "ORDER_FULFILLMENT", "PICKUP_VERIFY_FAILED", "Store", storeId, reason, null);
        } catch (Exception e) {
            log.warn("记录核销失败审计日志失败 tenantId={}, storeId={}, operatorId={}", tenantId, storeId, operatorId, e);
        }
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
