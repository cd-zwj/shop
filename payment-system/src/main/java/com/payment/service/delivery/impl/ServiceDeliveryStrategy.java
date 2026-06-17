package com.payment.service.delivery.impl;

import com.payment.entity.Product;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.ProductTypeEnum;
import com.payment.service.delivery.DeliveryResult;
import com.payment.service.delivery.DeliveryStrategy;
import com.payment.util.JsonUtils;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;

/**
 * 服务类交付策略。
 *
 * 支付成功后生成 6 位核销码并标记为 DELIVERED，商户侧通过核销接口确认服务消费。
 */
@Component
public class ServiceDeliveryStrategy implements DeliveryStrategy {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public ProductTypeEnum supports() {
        return ProductTypeEnum.SERVICE;
    }

    @Override
    public DeliveryResult deliver(SalesOrder order, SalesOrderItem item, Product product) {
        String verifyCode = String.format("%06d", RANDOM.nextInt(1_000_000));
        Map<String, Object> payload = Map.of(
                "verifyCode", verifyCode,
                "placeholder", false
        );
        return DeliveryResult.delivered(JsonUtils.toJson(payload));
    }
}
