package com.payment.service.delivery.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.payment.dto.CardKeyDeliveryDTO;
import com.payment.entity.OrderDeliveryRecord;
import com.payment.entity.Product;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.ProductTypeEnum;
import com.payment.service.CardKeyPoolService;
import com.payment.service.delivery.DeliveryResult;
import com.payment.service.delivery.DeliveryStrategy;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 卡密交付策略：从 card_key_pool 原子锁定一张可用卡密，退款撤销时标记 RETURNED。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardKeyDeliveryStrategy implements DeliveryStrategy {

    private final CardKeyPoolService cardKeyPoolService;

    @Override
    public ProductTypeEnum supports() {
        return ProductTypeEnum.CARD_KEY;
    }

    @Override
    public DeliveryResult deliver(SalesOrder order, SalesOrderItem item, Product product) {
        try {
            CardKeyDeliveryDTO cardKey = cardKeyPoolService.lockForDelivery(
                    order.getTenantId(), item.getProductId(), order.getOrderNo(), item.getId());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("cardKeyId", cardKey.getCardKeyId());
            payload.put("code", cardKey.getCode());
            payload.put("placeholder", false);
            return DeliveryResult.delivered(JsonUtils.toJson(payload));
        } catch (Exception e) {
            log.warn("CARD_KEY delivery failed, tenantId={}, productId={}, orderNo={}, itemId={}, reason={}",
                    order.getTenantId(), item.getProductId(), order.getOrderNo(), item.getId(), e.getMessage());
            return DeliveryResult.failed(e.getMessage() == null ? "卡密交付失败" : e.getMessage());
        }
    }

    @Override
    public void revoke(OrderDeliveryRecord record) {
        Long cardKeyId = extractCardKeyId(record.getPayload());
        if (cardKeyId != null) {
            cardKeyPoolService.returnByCardKeyId(record.getTenantId(), cardKeyId, "订单退款撤销交付");
            return;
        }
        cardKeyPoolService.returnByOrderItem(record.getTenantId(), record.getOrderItemId(), "订单退款撤销交付");
    }

    private Long extractCardKeyId(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> data = JsonUtils.fromJson(payload, new TypeReference<Map<String, Object>>() {});
            Object rawId = data.get("cardKeyId");
            if (rawId instanceof Number number) {
                return number.longValue();
            }
            if (rawId instanceof String text && !text.isBlank()) {
                return Long.valueOf(text);
            }
        } catch (Exception ex) {
            log.warn("CARD_KEY revoke payload parse failed, payload={}", payload, ex);
        }
        return null;
    }
}
