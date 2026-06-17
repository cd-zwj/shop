package com.payment.service.delivery.impl;

import com.payment.common.BusinessException;
import com.payment.dto.CardKeyDeliveryDTO;
import com.payment.entity.OrderDeliveryRecord;
import com.payment.entity.Product;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.DeliveryStatusEnum;
import com.payment.service.CardKeyPoolService;
import com.payment.service.delivery.DeliveryResult;
import com.payment.util.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhysicalDeliveryStrategyTest {

    private final PhysicalDeliveryStrategy strategy = new PhysicalDeliveryStrategy();

    @Test
    void deliverShouldReturnPendingWithoutPayload() {
        DeliveryResult result = strategy.deliver(new SalesOrder(), new SalesOrderItem(), new Product());
        assertNotNull(result);
        assertEquals(DeliveryStatusEnum.PENDING, result.status());
        // payload 暂为空，等商户后续点"发货"再回填物流单号
        assertNull(result.payload());
    }
}

class VirtualDeliveryStrategyTest {

    private final VirtualDeliveryStrategy strategy = new VirtualDeliveryStrategy();

    @Test
    void deliverShouldFailWhenConfigMissing() {
        DeliveryResult result = strategy.deliver(orderOf("O1"), itemOf(1L, "VIRTUAL"), productOf(null));
        assertEquals(DeliveryStatusEnum.FAILED, result.status());
        assertNotNull(result.failReason());
    }

    @Test
    void deliverShouldDeliverWithContentUrl() {
        Product product = productOf("{\"contentUrl\":\"https://example.com/file.zip\"}");
        DeliveryResult result = strategy.deliver(orderOf("O2"), itemOf(2L, "VIRTUAL"), product);
        assertEquals(DeliveryStatusEnum.DELIVERED, result.status());
        assertNotNull(result.payload());
        assertTrue(result.payload().contains("contentUrl"));
        assertTrue(result.payload().contains("example.com"));
    }

    @Test
    void deliverShouldDeliverWithAccountInfoOnly() {
        Product product = productOf("{\"accountInfo\":\"user:vip001 / pwd:hello\"}");
        DeliveryResult result = strategy.deliver(orderOf("O3"), itemOf(3L, "VIRTUAL"), product);
        assertEquals(DeliveryStatusEnum.DELIVERED, result.status());
        assertTrue(result.payload().contains("accountInfo"));
    }

    @Test
    void deliverShouldFailWhenConfigEmptyJson() {
        Product product = productOf("{}");
        DeliveryResult result = strategy.deliver(orderOf("O4"), itemOf(4L, "VIRTUAL"), product);
        assertEquals(DeliveryStatusEnum.FAILED, result.status());
    }

    private SalesOrder orderOf(String orderNo) {
        SalesOrder o = new SalesOrder();
        o.setOrderNo(orderNo);
        return o;
    }

    private SalesOrderItem itemOf(Long id, String type) {
        SalesOrderItem item = new SalesOrderItem();
        item.setId(id);
        item.setProductType(type);
        return item;
    }

    private Product productOf(String config) {
        Product p = new Product();
        p.setDeliveryConfig(config);
        return p;
    }
}

class CardKeyDeliveryStrategyTest {

    private final CardKeyPoolService cardKeyPoolService = mock(CardKeyPoolService.class);
    private final CardKeyDeliveryStrategy strategy = new CardKeyDeliveryStrategy(cardKeyPoolService);

    @Test
    void deliverShouldFailWhenPoolEmpty() {
        SalesOrder order = orderOf("O5");
        SalesOrderItem item = itemOf(5L, "CARD_KEY");
        item.setProductId(3L);
        when(cardKeyPoolService.lockForDelivery(7L, 3L, "O5", 5L))
                .thenThrow(new BusinessException("卡密库存不足"));

        DeliveryResult result = strategy.deliver(order, item, productOf(null));

        assertEquals(DeliveryStatusEnum.FAILED, result.status());
        assertEquals("卡密库存不足", result.failReason());
    }

    @Test
    void deliverShouldLockCodeFromPool() {
        SalesOrder order = orderOf("O6");
        SalesOrderItem item = itemOf(6L, "CARD_KEY");
        item.setProductId(3L);
        when(cardKeyPoolService.lockForDelivery(7L, 3L, "O6", 6L))
                .thenReturn(new CardKeyDeliveryDTO(101L, "VIP-2026-0001"));

        DeliveryResult result = strategy.deliver(order, item, productOf(null));

        assertEquals(DeliveryStatusEnum.DELIVERED, result.status());
        Map<String, Object> payload = JsonUtils.fromJson(result.payload(), Map.class);
        assertEquals(101, payload.get("cardKeyId"));
        assertEquals("VIP-2026-0001", payload.get("code"));
        assertEquals(false, payload.get("placeholder"));
    }

    @Test
    void revokeShouldReturnByCardKeyIdFromPayload() {
        OrderDeliveryRecord record = new OrderDeliveryRecord();
        record.setTenantId(7L);
        record.setOrderItemId(66L);
        record.setPayload("{\"cardKeyId\":101,\"code\":\"VIP-2026-0001\"}");

        strategy.revoke(record);

        verify(cardKeyPoolService).returnByCardKeyId(7L, 101L, "订单退款撤销交付");
    }

    @Test
    void revokeShouldFallbackToOrderItemWhenPayloadMissingCardKeyId() {
        OrderDeliveryRecord record = new OrderDeliveryRecord();
        record.setTenantId(7L);
        record.setOrderItemId(66L);
        record.setPayload("{\"code\":\"VIP-2026-0001\"}");

        strategy.revoke(record);

        verify(cardKeyPoolService).returnByOrderItem(7L, 66L, "订单退款撤销交付");
    }

    private SalesOrder orderOf(String orderNo) {
        SalesOrder o = new SalesOrder();
        o.setTenantId(7L);
        o.setOrderNo(orderNo);
        return o;
    }

    private SalesOrderItem itemOf(Long id, String type) {
        SalesOrderItem item = new SalesOrderItem();
        item.setId(id);
        item.setProductType(type);
        return item;
    }

    private Product productOf(String config) {
        Product p = new Product();
        p.setDeliveryConfig(config);
        return p;
    }
}
