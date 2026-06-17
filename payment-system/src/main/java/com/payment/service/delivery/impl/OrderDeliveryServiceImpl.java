package com.payment.service.delivery.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.payment.common.BusinessException;
import com.payment.config.RabbitMQConfig;
import com.payment.entity.MessageOutbox;
import com.payment.entity.OrderDeliveryRecord;
import com.payment.entity.Product;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.DeliveryStatusEnum;
import com.payment.enums.ProductTypeEnum;
import com.payment.mapper.OrderDeliveryRecordMapper;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.OutboxPublisher;
import com.payment.service.UserNotificationService;
import com.payment.service.delivery.DeliveryResult;
import com.payment.service.delivery.DeliveryStrategy;
import com.payment.service.delivery.DeliveryStrategyRegistry;
import com.payment.service.delivery.OrderDeliveryService;
import com.payment.service.outbox.OutboxMessageCommand;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDeliveryServiceImpl implements OrderDeliveryService {

    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final ProductMapper productMapper;
    private final OrderDeliveryRecordMapper deliveryRecordMapper;
    private final OutboxPublisher outboxPublisher;
    private final DeliveryStrategyRegistry strategyRegistry;
    private final UserNotificationService notificationService;

    // ----------------- 入队 -----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enqueueDelivery(String orderNo) {
        Map<String, Object> body = Map.of("bizNo", orderNo, "bizType", "ORDER_DELIVERY");
        MessageOutbox outbox = outboxPublisher.publish(OutboxMessageCommand.builder()
                .messagePrefix("DLV")
                .bizType("ORDER_DELIVERY")
                .bizNo(orderNo)
                .routingKey(RabbitMQConfig.V1_ORDER_DELIVERY_QUEUE)
                .messageBody(body)
                .build());
        log.info("Delivery enqueued, orderNo={}, outboxId={}", orderNo, outbox.getId());
    }

    // ----------------- 主流程 -----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverOrder(String orderNo) {
        SalesOrder order = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderNo, orderNo)
                .eq(SalesOrder::getDeleted, 0));
        if (order == null) {
            log.warn("交付订单不存在,跳过 orderNo={}", orderNo);
            return;
        }
        List<SalesOrderItem> items = salesOrderItemMapper.selectByOrderId(order.getId());
        if (items.isEmpty()) {
            log.warn("交付订单无明细,跳过 orderNo={}", orderNo);
            return;
        }

        for (SalesOrderItem item : items) {
            try {
                deliverItem(order, item);
            } catch (Exception e) {
                log.error("订单项交付失败 orderNo={}, itemId={}", orderNo, item.getId(), e);
                markItemFailed(item, e.getMessage());
            }
        }
    }

    private void deliverItem(SalesOrder order, SalesOrderItem item) {
        // 幂等：已交付/已确认/已撤销直接跳过
        if (item.getDeliveryStatus() != null) {
            DeliveryStatusEnum current;
            try {
                current = DeliveryStatusEnum.valueOf(item.getDeliveryStatus());
            } catch (IllegalArgumentException ex) {
                current = DeliveryStatusEnum.PENDING;
            }
            if (current == DeliveryStatusEnum.DELIVERED
                    || current == DeliveryStatusEnum.CONFIRMED
                    || current == DeliveryStatusEnum.REVOKED) {
                return;
            }
        }

        ProductTypeEnum type = resolveProductType(item);
        DeliveryStrategy strategy = strategyRegistry.getOrDefault(type);
        if (strategy == null) {
            log.error("找不到任何 DeliveryStrategy, 兜底失败 itemId={}, type={}", item.getId(), type);
            markItemFailed(item, "无可用交付策略");
            return;
        }

        Product product = productMapper.selectById(item.getProductId());
        DeliveryResult result = strategy.deliver(order, item, product);
        if (result == null) {
            markItemFailed(item, "策略返回空结果");
            return;
        }

        OrderDeliveryRecord record = upsertRecord(order, item, type, result);
        applyItemStatus(item, result.status(), result.status() == DeliveryStatusEnum.DELIVERED);

        if (result.status() == DeliveryStatusEnum.DELIVERED) {
            sendDeliveredNotification(order, item, record);
        }
    }

    private ProductTypeEnum resolveProductType(SalesOrderItem item) {
        String raw = item.getProductType();
        if (raw == null || raw.isBlank()) {
            return ProductTypeEnum.PHYSICAL;
        }
        try {
            return ProductTypeEnum.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            log.warn("非法 productType={}, fallback to PHYSICAL, itemId={}", raw, item.getId());
            return ProductTypeEnum.PHYSICAL;
        }
    }

    private OrderDeliveryRecord upsertRecord(SalesOrder order,
                                             SalesOrderItem item,
                                             ProductTypeEnum type,
                                             DeliveryResult result) {
        OrderDeliveryRecord existing = deliveryRecordMapper.selectOne(new LambdaQueryWrapper<OrderDeliveryRecord>()
                .eq(OrderDeliveryRecord::getOrderItemId, item.getId())
                .eq(OrderDeliveryRecord::getDeleted, 0));

        if (existing == null) {
            OrderDeliveryRecord fresh = newRecord(order, item, type, result, 0);
            try {
                deliveryRecordMapper.insert(fresh);
                return fresh;
            } catch (DuplicateKeyException ex) {
                // 并发场景:另一线程刚插入,回退到 UPDATE。uk_tenant_item 唯一索引兜底。
                log.info("交付记录并发冲突,回退到更新 itemId={}", item.getId());
                existing = deliveryRecordMapper.selectOne(new LambdaQueryWrapper<OrderDeliveryRecord>()
                        .eq(OrderDeliveryRecord::getOrderItemId, item.getId())
                        .eq(OrderDeliveryRecord::getDeleted, 0));
                if (existing == null) {
                    throw ex;
                }
            }
        }

        // 已存在:增加重试计数后更新
        existing.setTenantId(order.getTenantId());
        existing.setOrderId(order.getId());
        existing.setOrderNo(order.getOrderNo());
        existing.setPlatformUserId(order.getPlatformUserId());
        existing.setProductId(item.getProductId());
        existing.setProductType(type.name());
        existing.setStatus(result.status().name());
        existing.setPayload(result.payload());
        existing.setFailReason(result.failReason());
        existing.setRetryCount((existing.getRetryCount() == null ? 0 : existing.getRetryCount()) + 1);
        if (result.status() == DeliveryStatusEnum.DELIVERED) {
            existing.setDeliveredTime(LocalDateTime.now());
        }
        deliveryRecordMapper.updateById(existing);
        return existing;
    }

    private OrderDeliveryRecord newRecord(SalesOrder order,
                                          SalesOrderItem item,
                                          ProductTypeEnum type,
                                          DeliveryResult result,
                                          int retryCount) {
        OrderDeliveryRecord record = new OrderDeliveryRecord();
        record.setTenantId(order.getTenantId());
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setOrderItemId(item.getId());
        record.setPlatformUserId(order.getPlatformUserId());
        record.setProductId(item.getProductId());
        record.setProductType(type.name());
        record.setStatus(result.status().name());
        record.setPayload(result.payload());
        record.setFailReason(result.failReason());
        record.setRetryCount(retryCount);
        if (result.status() == DeliveryStatusEnum.DELIVERED) {
            record.setDeliveredTime(LocalDateTime.now());
        }
        return record;
    }

    private void applyItemStatus(SalesOrderItem item, DeliveryStatusEnum status, boolean stampDeliveredTime) {
        LambdaUpdateWrapper<SalesOrderItem> update = new LambdaUpdateWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getId, item.getId())
                .set(SalesOrderItem::getDeliveryStatus, status.name());
        if (stampDeliveredTime) {
            update.set(SalesOrderItem::getDeliveredTime, LocalDateTime.now());
        }
        salesOrderItemMapper.update(null, update);
    }

    private void markItemFailed(SalesOrderItem item, String reason) {
        salesOrderItemMapper.update(null, new LambdaUpdateWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getId, item.getId())
                .set(SalesOrderItem::getDeliveryStatus, DeliveryStatusEnum.FAILED.name()));
        log.warn("订单项交付失败已落库 itemId={}, reason={}", item.getId(), reason);
    }

    private void sendDeliveredNotification(SalesOrder order, SalesOrderItem item, OrderDeliveryRecord record) {
        try {
            notificationService.send(
                    order.getPlatformUserId(),
                    "商品已交付",
                    "您购买的「" + (item.getProductName() == null ? "商品" : item.getProductName())
                            + "」已交付,订单号 " + order.getOrderNo() + ",请在「我的已购」中查看",
                    "ORDER");
        } catch (Exception e) {
            // 通知失败不影响交付主流程
            log.warn("交付通知发送失败 orderNo={}, recordId={}", order.getOrderNo(), record.getId(), e);
        }
    }

    // ----------------- C 端查询 -----------------

    @Override
    public Page<OrderDeliveryRecord> listUserDeliveries(Long platformUserId, String status, Integer current, Integer size) {
        int page = current == null || current < 1 ? 1 : current;
        int sz = size == null || size < 1 ? 10 : Math.min(size, 100);
        LambdaQueryWrapper<OrderDeliveryRecord> q = new LambdaQueryWrapper<OrderDeliveryRecord>()
                .eq(OrderDeliveryRecord::getPlatformUserId, platformUserId)
                .eq(OrderDeliveryRecord::getDeleted, 0)
                .orderByDesc(OrderDeliveryRecord::getCreateTime);
        if (status != null && !status.isBlank()) {
            q.eq(OrderDeliveryRecord::getStatus, status);
        }
        return deliveryRecordMapper.selectPage(new Page<>(page, sz), q);
    }

    @Override
    public OrderDeliveryRecord getUserDelivery(Long platformUserId, Long recordId) {
        OrderDeliveryRecord record = deliveryRecordMapper.selectById(recordId);
        if (record == null || record.getDeleted() != null && record.getDeleted() == 1) {
            throw new BusinessException("交付记录不存在");
        }
        if (!record.getPlatformUserId().equals(platformUserId)) {
            throw new BusinessException("无权访问该交付记录");
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDeliveryRecord confirmReceived(Long platformUserId, Long recordId) {
        OrderDeliveryRecord record = getUserDelivery(platformUserId, recordId);
        if (DeliveryStatusEnum.CONFIRMED.name().equals(record.getStatus())) {
            return record;
        }
        if (!DeliveryStatusEnum.DELIVERED.name().equals(record.getStatus())) {
            throw new BusinessException("只有已交付的记录可以确认");
        }
        record.setStatus(DeliveryStatusEnum.CONFIRMED.name());
        record.setConfirmedTime(LocalDateTime.now());
        deliveryRecordMapper.updateById(record);

        salesOrderItemMapper.update(null, new LambdaUpdateWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getId, record.getOrderItemId())
                .set(SalesOrderItem::getDeliveryStatus, DeliveryStatusEnum.CONFIRMED.name()));
        return record;
    }

    // ----------------- B 端发货 -----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDeliveryRecord markShipped(Long tenantId, Long orderItemId, String shippingNo, String logisticsCompany) {
        if (shippingNo == null || shippingNo.isBlank()) {
            throw new BusinessException("物流单号不能为空");
        }
        SalesOrderItem item = salesOrderItemMapper.selectById(orderItemId);
        if (item == null) {
            throw new BusinessException("订单项不存在");
        }
        if (!item.getTenantId().equals(tenantId)) {
            throw new BusinessException("无权操作该订单项");
        }
        if (!ProductTypeEnum.PHYSICAL.name().equals(item.getProductType())) {
            throw new BusinessException("仅实物商品需要发货");
        }

        OrderDeliveryRecord record = deliveryRecordMapper.selectOne(new LambdaQueryWrapper<OrderDeliveryRecord>()
                .eq(OrderDeliveryRecord::getOrderItemId, orderItemId)
                .eq(OrderDeliveryRecord::getDeleted, 0));
        if (record == null) {
            throw new BusinessException("交付记录不存在,请稍后再试");
        }
        if (DeliveryStatusEnum.CONFIRMED.name().equals(record.getStatus())
                || DeliveryStatusEnum.REVOKED.name().equals(record.getStatus())) {
            throw new BusinessException("当前状态不允许发货");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("shippingNo", shippingNo);
        if (logisticsCompany != null && !logisticsCompany.isBlank()) {
            payload.put("logisticsCompany", logisticsCompany);
        }
        record.setPayload(JsonUtils.toJson(payload));
        record.setStatus(DeliveryStatusEnum.DELIVERED.name());
        record.setDeliveredTime(LocalDateTime.now());
        deliveryRecordMapper.updateById(record);

        salesOrderItemMapper.update(null, new LambdaUpdateWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getId, orderItemId)
                .set(SalesOrderItem::getDeliveryStatus, DeliveryStatusEnum.DELIVERED.name())
                .set(SalesOrderItem::getDeliveredTime, LocalDateTime.now()));

        try {
            SalesOrder order = salesOrderMapper.selectById(item.getOrderId());
            sendDeliveredNotification(order, item, record);
        } catch (Exception e) {
            log.warn("实物发货通知失败 itemId={}", orderItemId, e);
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDeliveryRecord verifyService(Long tenantId, String verifyCode) {
        if (verifyCode == null || verifyCode.isBlank()) {
            throw new BusinessException("核销码不能为空");
        }
        String normalizedCode = verifyCode.trim();
        OrderDeliveryRecord record = deliveryRecordMapper.selectOne(new LambdaQueryWrapper<OrderDeliveryRecord>()
                .eq(OrderDeliveryRecord::getTenantId, tenantId)
                .eq(OrderDeliveryRecord::getProductType, ProductTypeEnum.SERVICE.name())
                .eq(OrderDeliveryRecord::getDeleted, 0)
                .apply("JSON_UNQUOTE(JSON_EXTRACT(payload, '$.verifyCode')) = {0}", normalizedCode));
        if (record == null) {
            throw new BusinessException("核销码不存在或不属于当前商户");
        }
        if (!normalizedCode.equals(readVerifyCode(record.getPayload()))) {
            throw new BusinessException("核销码不存在或不属于当前商户");
        }
        if (DeliveryStatusEnum.CONFIRMED.name().equals(record.getStatus())) {
            return record;
        }
        if (!DeliveryStatusEnum.DELIVERED.name().equals(record.getStatus())) {
            throw new BusinessException("当前交付状态不允许核销");
        }

        record.setStatus(DeliveryStatusEnum.CONFIRMED.name());
        record.setConfirmedTime(LocalDateTime.now());
        deliveryRecordMapper.updateById(record);

        salesOrderItemMapper.update(null, new LambdaUpdateWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getId, record.getOrderItemId())
                .set(SalesOrderItem::getDeliveryStatus, DeliveryStatusEnum.CONFIRMED.name()));
        return record;
    }

    private String readVerifyCode(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            JsonNode node = JsonUtils.fromJsonTree(payload);
            if (!node.hasNonNull("verifyCode")) {
                return null;
            }
            String code = node.get("verifyCode").asText();
            return code.isBlank() ? null : code;
        } catch (Exception e) {
            log.warn("服务核销 payload 解析失败 payload={}", payload, e);
            return null;
        }
    }

    // ----------------- 退款回收 -----------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OrderDeliveryRecord> revokeByOrderItem(Long orderItemId) {
        List<OrderDeliveryRecord> records = deliveryRecordMapper.selectList(new LambdaQueryWrapper<OrderDeliveryRecord>()
                .eq(OrderDeliveryRecord::getOrderItemId, orderItemId)
                .eq(OrderDeliveryRecord::getDeleted, 0));
        return revokeRecords(records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OrderDeliveryRecord> revokeByOrderNo(String orderNo) {
        List<OrderDeliveryRecord> records = deliveryRecordMapper.selectList(new LambdaQueryWrapper<OrderDeliveryRecord>()
                .eq(OrderDeliveryRecord::getOrderNo, orderNo)
                .eq(OrderDeliveryRecord::getDeleted, 0));
        return revokeRecords(records);
    }

    private List<OrderDeliveryRecord> revokeRecords(List<OrderDeliveryRecord> records) {
        List<OrderDeliveryRecord> changed = new ArrayList<>();
        for (OrderDeliveryRecord record : records) {
            if (DeliveryStatusEnum.REVOKED.name().equals(record.getStatus())) {
                continue;
            }
            boolean revokeOk = true;
            String failReason = null;
            try {
                ProductTypeEnum type = ProductTypeEnum.valueOf(record.getProductType());
                DeliveryStrategy strategy = strategyRegistry.get(type);
                if (strategy != null) {
                    strategy.revoke(record);
                }
            } catch (Exception e) {
                revokeOk = false;
                failReason = "策略撤销失败: " + e.getMessage();
                log.error("策略 revoke 失败 recordId={}, 资源可能未真正回收,请人工介入", record.getId(), e);
            }

            if (revokeOk) {
                record.setStatus(DeliveryStatusEnum.REVOKED.name());
                record.setRevokedTime(LocalDateTime.now());
                deliveryRecordMapper.updateById(record);
                salesOrderItemMapper.update(null, new LambdaUpdateWrapper<SalesOrderItem>()
                        .eq(SalesOrderItem::getId, record.getOrderItemId())
                        .set(SalesOrderItem::getDeliveryStatus, DeliveryStatusEnum.REVOKED.name()));
            } else {
                // 资源未回收 — 记录失败状态等人工介入,不修改 item.deliveryStatus(避免下游误以为已撤销)
                record.setStatus(DeliveryStatusEnum.REVOKE_FAILED.name());
                record.setFailReason(failReason);
                deliveryRecordMapper.updateById(record);
            }
            changed.add(record);
        }
        return changed;
    }
}
