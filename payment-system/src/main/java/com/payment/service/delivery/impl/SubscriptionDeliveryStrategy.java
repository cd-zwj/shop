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
import java.time.LocalDateTime;

/**
 * 订阅 / 权益包交付策略。
 *
 * 当前本地闭环生成可追溯的激活与到期信息，后续真实权益判定可复用 payload 中的 benefitCode。
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

        LocalDateTime activatedTime = LocalDateTime.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("validityDays", validityDays);
        payload.put("activatedTime", activatedTime.toString());
        payload.put("expireTime", activatedTime.plusDays(validityDays).toString());
        payload.put("placeholder", false);
        payload.put("note", "订阅权益已激活，可在有效期内重复查看。");
        if (config != null && !config.isBlank()) {
            try {
                JsonNode cfg = JsonUtils.fromJsonTree(config);
                if (cfg != null && cfg.hasNonNull("benefitCode")) {
                    payload.put("benefitCode", cfg.get("benefitCode").asText());
                }
            } catch (Exception ignored) {
                // invalid config was already logged while resolving validityDays
            }
        }
        return DeliveryResult.delivered(JsonUtils.toJson(payload));
    }
}
