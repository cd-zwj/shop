package com.payment.service.delivery.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.payment.entity.Product;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.DeliveryStatusEnum;
import com.payment.service.delivery.DeliveryResult;
import com.payment.util.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionDeliveryStrategyTest {

    @Test
    void deliverShouldActivateSubscriptionWithoutPlaceholderFlag() {
        Product product = new Product();
        product.setId(7L);
        product.setDeliveryConfig("{\"validityDays\":45,\"benefitCode\":\"VIP_PLUS\"}");
        SalesOrder order = new SalesOrder();
        order.setOrderNo("SO202607070001");
        SalesOrderItem item = new SalesOrderItem();
        item.setProductId(7L);

        DeliveryResult result = new SubscriptionDeliveryStrategy().deliver(order, item, product);

        JsonNode payload = JsonUtils.fromJsonTree(result.payload());
        assertEquals(DeliveryStatusEnum.DELIVERED, result.status());
        assertEquals(45, payload.get("validityDays").asInt());
        assertEquals("VIP_PLUS", payload.get("benefitCode").asText());
        assertTrue(payload.hasNonNull("activatedTime"));
        assertTrue(payload.hasNonNull("expireTime"));
        assertFalse(payload.path("placeholder").asBoolean(true));
    }
}
