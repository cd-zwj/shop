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
 * 虚拟内容交付策略。
 *
 * 从 {@code Product.deliveryConfig} 解析出 contentUrl / accountInfo，立即标记 DELIVERED。
 * 第一版直接给明文 URL，后续会迭代为签名 URL + 防盗链 + 下载次数限制。
 */
@Slf4j
@Component
public class VirtualDeliveryStrategy implements DeliveryStrategy {

    @Override
    public ProductTypeEnum supports() {
        return ProductTypeEnum.VIRTUAL;
    }

    @Override
    public DeliveryResult deliver(SalesOrder order, SalesOrderItem item, Product product) {
        String config = product == null ? null : product.getDeliveryConfig();
        if (config == null || config.isBlank()) {
            log.warn("VIRTUAL product missing deliveryConfig, productId={}, orderNo={}", item.getProductId(), order.getOrderNo());
            return DeliveryResult.failed("商品未配置交付内容(deliveryConfig 缺失)");
        }

        try {
            JsonNode cfg = JsonUtils.fromJsonTree(config);
            String contentUrl = textOrNull(cfg, "contentUrl");
            String accountInfo = textOrNull(cfg, "accountInfo");
            if (contentUrl == null && accountInfo == null) {
                return DeliveryResult.failed("deliveryConfig 缺少 contentUrl/accountInfo");
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            if (contentUrl != null) {
                payload.put("contentUrl", contentUrl);
            }
            if (accountInfo != null) {
                payload.put("accountInfo", accountInfo);
            }
            return DeliveryResult.delivered(JsonUtils.toJson(payload));
        } catch (Exception e) {
            log.error("VIRTUAL delivery payload build failed, productId={}, orderNo={}", item.getProductId(), order.getOrderNo(), e);
            return DeliveryResult.failed("交付配置解析失败: " + e.getMessage());
        }
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        String text = node.get(field).asText();
        return text.isBlank() ? null : text;
    }
}
