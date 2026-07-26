package com.payment.service.delivery.impl;

import com.payment.common.BusinessException;
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
        private final OrderDeliveryServiceImpl service = new OrderDeliveryServiceImpl(
                salesOrderMapper,
                salesOrderItemMapper,
                deliveryRecordMapper,
                actionMapper,
                outboxPublisher,
                notificationService,
                auditLogService);
    }
}
