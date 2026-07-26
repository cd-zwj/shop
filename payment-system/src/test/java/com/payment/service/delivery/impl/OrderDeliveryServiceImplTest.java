package com.payment.service.delivery.impl;

import com.payment.common.BusinessException;
import com.payment.entity.OrderDeliveryRecord;
import com.payment.entity.OrderFulfillmentAction;
import com.payment.entity.SalesOrder;
import com.payment.mapper.OrderDeliveryRecordMapper;
import com.payment.mapper.OrderFulfillmentActionMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
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

        fixture.service.verifyPickup(9L, 7L, "12345678", 100L);

        assertEquals("CONFIRMED", record.getStatus());
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

        verify(fixture.deliveryRecordMapper, never()).updateById(any(OrderDeliveryRecord.class));
        verify(fixture.actionMapper, never()).insert(any(OrderFulfillmentAction.class));
    }

    private static OrderDeliveryRecord record() {
        OrderDeliveryRecord record = new OrderDeliveryRecord();
        record.setId(1L);
        record.setTenantId(9L);
        record.setOrderId(3L);
        record.setOrderNo("SO001");
        record.setOrderItemId(11L);
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
        private final OrderDeliveryServiceImpl service = new OrderDeliveryServiceImpl(
                salesOrderMapper,
                salesOrderItemMapper,
                deliveryRecordMapper,
                actionMapper,
                outboxPublisher,
                notificationService);
    }
}
