package com.payment.service.delivery.impl;

import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.entity.OrderDeliveryRecord;
import com.payment.entity.OrderFulfillmentAction;
import com.payment.entity.SalesOrder;
import com.payment.mapper.OrderDeliveryRecordMapper;
import com.payment.mapper.OrderFulfillmentActionMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.AuditLogService;
import com.payment.service.OutboxPublisher;
import com.payment.service.UserNotificationService;
import com.payment.service.MerchantStoreScope;
import com.payment.service.delivery.PickupCodePayloadService;
import com.payment.service.impl.MerchantStoreScopeService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class OrderDeliveryServiceImplTest {

    @BeforeAll
    static void initializeTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), SalesOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), OrderDeliveryRecord.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), com.payment.entity.SalesOrderItem.class);
    }

    @Test
    void verifyPickupShouldWriteAuditWithoutChangingCompletedOrderStatus() {
        Fixture fixture = new Fixture();
        OrderDeliveryRecord record = record();
        SalesOrder order = order("COMPLETED");
        when(fixture.deliveryRecordMapper.selectOne(any())).thenReturn(record);
        when(fixture.salesOrderMapper.selectById(3L)).thenReturn(order);
        when(fixture.deliveryRecordMapper.update(any(), any())).thenReturn(1);

        OrderDeliveryRecord result = fixture.service.verifyPickup(9L, 7L, "12345678", 100L);

        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(100L, result.getVerifiedBy());
        assertEquals("COMPLETED", order.getOrderStatus());
        ArgumentCaptor<OrderFulfillmentAction> captor = ArgumentCaptor.forClass(OrderFulfillmentAction.class);
        verify(fixture.actionMapper).insert(captor.capture());
        assertEquals("PICKUP_VERIFIED", captor.getValue().getAction());
        assertEquals(100L, captor.getValue().getOperatorId());
    }

    @Test
    void verifyPickupShouldRejectOrderBeforePreparationIsCompleted() {
        Fixture fixture = new Fixture();
        OrderDeliveryRecord record = record();
        when(fixture.deliveryRecordMapper.selectOne(any())).thenReturn(record);
        when(fixture.salesOrderMapper.selectById(3L)).thenReturn(order("PREPARING"));

        assertThrows(BusinessException.class, () -> fixture.service.verifyPickup(9L, 7L, "12345678", 100L));

        verify(fixture.deliveryRecordMapper, never()).update(any(), any());
        verify(fixture.actionMapper, never()).insert(any(OrderFulfillmentAction.class));
        verify(fixture.auditLogService).log(eq(9L), eq(100L), eq("MERCHANT"), any(),
                eq("ORDER_FULFILLMENT"), eq("PICKUP_VERIFY_FAILED"), eq("Store"), eq(7L), any(), any());
    }

    @Test
    void verifyPickupShouldRejectStoreMismatchAndAuditFailure() {
        Fixture fixture = new Fixture();
        OrderDeliveryRecord record = record();
        record.setStoreId(8L);
        when(fixture.deliveryRecordMapper.selectOne(any())).thenReturn(record);

        assertThrows(BusinessException.class, () -> fixture.service.verifyPickup(9L, 7L, "12345678", 100L));

        verify(fixture.deliveryRecordMapper, never()).update(any(), any());
        verify(fixture.auditLogService).log(eq(9L), eq(100L), eq("MERCHANT"), any(),
                eq("ORDER_FULFILLMENT"), eq("PICKUP_VERIFY_FAILED"), eq("Store"), eq(7L), any(), any());
    }

    @Test
    void verifyPickupShouldRejectRecordWithoutStoreAssignment() {
        Fixture fixture = new Fixture();
        OrderDeliveryRecord record = record();
        record.setStoreId(null);
        when(fixture.deliveryRecordMapper.selectOne(any())).thenReturn(record);

        assertThrows(BusinessException.class, () -> fixture.service.verifyPickup(9L, 7L, "12345678", 100L));

        verify(fixture.deliveryRecordMapper, never()).update(any(), any());
    }

    @Test
    void verifyPickupShouldReturnLatestRecordWhenConcurrentlyConfirmed() {
        Fixture fixture = new Fixture();
        OrderDeliveryRecord record = record();
        when(fixture.deliveryRecordMapper.selectOne(any())).thenReturn(record);
        when(fixture.salesOrderMapper.selectById(3L)).thenReturn(order("COMPLETED"));
        // 并发场景：条件更新未命中（另一请求已核销），应幂等返回最新记录且不重复留痕。
        when(fixture.deliveryRecordMapper.update(any(), any())).thenReturn(0);
        OrderDeliveryRecord latest = record();
        latest.setStatus("CONFIRMED");
        latest.setVerifiedBy(200L);
        when(fixture.deliveryRecordMapper.selectById(anyLong())).thenReturn(latest);

        OrderDeliveryRecord result = fixture.service.verifyPickup(9L, 7L, "12345678", 100L);

        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(200L, result.getVerifiedBy());
        verify(fixture.actionMapper, never()).insert(any(OrderFulfillmentAction.class));
    }

    @Test
    void verifyPickupShouldRejectUnauthorizedStoreBeforeLookingUpPickupCode() {
        Fixture fixture = new Fixture(false);
        MerchantStoreScope scope = new MerchantStoreScope(9L, 3L, false, java.util.List.of(8L));
        when(fixture.scopeService.resolve(9L, 100L, MerchantPermission.ORDER_MANAGE)).thenReturn(scope);
        org.mockito.Mockito.doThrow(new BusinessException("当前员工无权访问该门店"))
                .when(fixture.scopeService).requireStoreAccess(scope, 7L);

        assertThrows(BusinessException.class, () -> fixture.service.verifyPickup(9L, 7L, "12345678", 100L));

        verify(fixture.deliveryRecordMapper, never()).selectOne(any());
        verify(fixture.deliveryRecordMapper, never()).update(any(), any());
    }

    @Test
    void deliverOrderShouldPersistEncryptedPickupPayload() {
        Fixture fixture = new Fixture();
        SalesOrder order = order("PAID");
        order.setOrderNo("SO001");
        order.setPlatformUserId(100L);
        order.setFulfillmentMode("STORE_PICKUP");
        com.payment.entity.SalesOrderItem item = new com.payment.entity.SalesOrderItem();
        item.setId(11L);
        item.setProductId(21L);
        item.setProductName("测试商品");
        when(fixture.salesOrderMapper.selectOne(any())).thenReturn(order);
        when(fixture.salesOrderItemMapper.selectByOrderId(3L)).thenReturn(java.util.List.of(item));
        when(fixture.payloadService.createEncryptedPayload(
                eq(9L), eq("SO001"), eq(11L), eq(7L), any()))
                .thenReturn("{\"pickupCodeCiphertext\":\"pc1.v1.cipher\",\"storeId\":7}");

        fixture.service.deliverOrder("SO001");

        ArgumentCaptor<OrderDeliveryRecord> captor = ArgumentCaptor.forClass(OrderDeliveryRecord.class);
        verify(fixture.deliveryRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getPayload()).doesNotContain("pickupCode\"");
        assertThat(captor.getValue().getPickupCodeHash()).hasSize(64);
    }

    @Test
    void getPickupCodesForUserShouldDecryptOnlyMatchingDeliveryRecords() {
        Fixture fixture = new Fixture();
        OrderDeliveryRecord record = record();
        record.setPlatformUserId(100L);
        record.setPayload("{\"pickupCodeCiphertext\":\"pc1.v1.cipher\"}");
        when(fixture.deliveryRecordMapper.selectList(any())).thenReturn(java.util.List.of(record));
        when(fixture.payloadService.readPickupCode(record)).thenReturn("12345678");

        java.util.Map<Long, String> result = fixture.service.getPickupCodesForUser(9L, 100L, "SO001");

        assertThat(result).containsExactly(java.util.Map.entry(11L, "12345678"));
    }

    private static OrderDeliveryRecord record() {
        OrderDeliveryRecord record = new OrderDeliveryRecord();
        record.setId(1L);
        record.setTenantId(9L);
        record.setOrderId(3L);
        record.setOrderNo("SO001");
        record.setOrderItemId(11L);
        record.setStoreId(7L);
        record.setStatus("DELIVERED");
        return record;
    }

    private static SalesOrder order(String status) {
        SalesOrder order = new SalesOrder();
        order.setId(3L);
        order.setTenantId(9L);
        order.setStoreId(7L);
        order.setOrderStatus(status);
        return order;
    }

    private static class Fixture {
        private final SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        private final SalesOrderItemMapper salesOrderItemMapper = mock(SalesOrderItemMapper.class);
        private final OrderDeliveryRecordMapper deliveryRecordMapper = mock(OrderDeliveryRecordMapper.class);
        private final OrderFulfillmentActionMapper actionMapper = mock(OrderFulfillmentActionMapper.class);
        private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
        private final UserNotificationService notificationService = mock(UserNotificationService.class);
        private final AuditLogService auditLogService = mock(AuditLogService.class);
        private final MerchantStoreScopeService scopeService = mock(MerchantStoreScopeService.class);
        private final PickupCodePayloadService payloadService = mock(PickupCodePayloadService.class);
        private final OrderDeliveryServiceImpl service = new OrderDeliveryServiceImpl(
                salesOrderMapper,
                salesOrderItemMapper,
                deliveryRecordMapper,
                actionMapper,
                outboxPublisher,
                notificationService,
                auditLogService,
                scopeService,
                payloadService);

        private Fixture() {
            this(true);
        }

        private Fixture(boolean allowDefaultStore) {
            if (allowDefaultStore) {
                MerchantStoreScope scope = new MerchantStoreScope(9L, 3L, false, java.util.List.of(7L));
                when(scopeService.resolve(9L, 100L, MerchantPermission.ORDER_MANAGE)).thenReturn(scope);
            }
        }
    }
}
