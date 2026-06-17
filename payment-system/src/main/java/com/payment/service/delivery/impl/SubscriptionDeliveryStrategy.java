package com.payment.service.delivery.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.payment.entity.Product;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.ProductTypeEnum;
import com.payment.service.delivery.DeliveryResult;
import com.payment.service.delivery.DeliveryStrategy;
import com.payment.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 订阅 / 权益包交付策略（占位）。
 *
 * 第一版仅从 deliveryConfig.validityDays 读出有效期天数，标 DELIVERED，
 * 真正的"权益判定 / 续费 / 到期降权"在后续批次补。
 */
@Slf4j
@Component
public class SubscriptionDeliveryStrategy implements DeliveryStrategy {

    private static final int DEFAULT_VALIDITY_DAYS = 30;

    @Override
    public ProductTypeEnum supports() {
        return ProductTypeEnum.SUBSCRIPTION;
    }

    @Override
    public DeliveryResult deliver(SalesOrder order, SalesOrderItem item, Product product) {
        int validityDays = DEFAULT_VALIDITY_DAYS;
        String config = product == null ? null : product.getDeliveryConfig();
        if (config != null && !config.isBlank()) {
            try {
                JsonNode cfg = JsonUtils.fromJsonTree(config);
                if (cfg != null && cfg.hasNonNull("validityDays")) {
                    int parsed = cfg.get("validityDays").asInt(DEFAULT_VALIDITY_DAYS);
                    if (parsed > 0) {
                        validityDays = parsed;
                    }
                }
            } catch (Exception e) {
                log.warn("SUBSCRIPTION deliveryConfig 解析失败, productId={}, fallback validityDays={}",
                        item.getProductId(), DEFAULT_VALIDITY_DAYS, e);
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("validityDays", validityDays);
        payload.put("placeholder", true);
        payload.put("note", "权益生命周期尚未接入,当前为占位实现");
        return DeliveryResult.delivered(JsonUtils.toJson(payload));
    }
}
