package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.entity.OrderFulfillmentAction;
import com.payment.entity.SalesOrder;
import com.payment.mapper.OrderFulfillmentActionMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.MerchantStoreScope;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderFulfillmentServiceImplTest {

    @BeforeAll
    static void initializeTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SalesOrder.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrderFulfillmentAction.class);
    }

    @Test
    void startPreparationShouldTransitionPaidPickupOrderAndWriteAuditAction() {
        SalesOrderMapper orderMapper = mock(SalesOrderMapper.class);
        OrderFulfillmentActionMapper actionMapper = mock(OrderFulfillmentActionMapper.class);
        MerchantStoreScopeService scopeService = mock(MerchantStoreScopeService.class);
        OrderFulfillmentServiceImpl service = new OrderFulfillmentServiceImpl(orderMapper, actionMapper, scopeService);
        SalesOrder order = order("PENDING_PREPARATION");
        MerchantStoreScope scope = new MerchantStoreScope(9L, 3L, false, java.util.List.of(7L));
        when(scopeService.resolve(9L, 100L, MerchantPermission.ORDER_MANAGE)).thenReturn(scope);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.update(isNull(), any())).thenReturn(1);

        service.startPreparation(9L, "SO001", 100L, "开始备货");

        ArgumentCaptor<OrderFulfillmentAction> captor = ArgumentCaptor.forClass(OrderFulfillmentAction.class);
        verify(actionMapper).insert(captor.capture());
        assertEquals("START_PREPARATION", captor.getValue().getAction());
        assertEquals("PENDING_PREPARATION", captor.getValue().getFromStatus());
        assertEquals("PREPARING", captor.getValue().getToStatus());
        assertEquals(100L, captor.getValue().getOperatorId());
    }

    @Test
    void completePreparationShouldRejectOrderThatHasNotStartedPreparation() {
        SalesOrderMapper orderMapper = mock(SalesOrderMapper.class);
        OrderFulfillmentActionMapper actionMapper = mock(OrderFulfillmentActionMapper.class);
        MerchantStoreScopeService scopeService = mock(MerchantStoreScopeService.class);
        OrderFulfillmentServiceImpl service = new OrderFulfillmentServiceImpl(orderMapper, actionMapper, scopeService);
        MerchantStoreScope scope = new MerchantStoreScope(9L, 3L, false, java.util.List.of(7L));
        when(scopeService.resolve(9L, 100L, MerchantPermission.ORDER_MANAGE)).thenReturn(scope);
        when(orderMapper.selectOne(any())).thenReturn(order("PENDING_PREPARATION"));

        assertThrows(BusinessException.class, () -> service.completePreparation(9L, "SO001", 100L, null));

        verify(orderMapper, never()).update(isNull(), any());
        verify(actionMapper, never()).insert(any(OrderFulfillmentAction.class));
    }

    @Test
    void startPreparationShouldRejectOrderOutsideAssignedScopeBeforeUpdate() {
        SalesOrderMapper orderMapper = mock(SalesOrderMapper.class);
        OrderFulfillmentActionMapper actionMapper = mock(OrderFulfillmentActionMapper.class);
        MerchantStoreScopeService scopeService = mock(MerchantStoreScopeService.class);
        OrderFulfillmentServiceImpl service = new OrderFulfillmentServiceImpl(orderMapper, actionMapper, scopeService);
        MerchantStoreScope scope = new MerchantStoreScope(9L, 3L, false, java.util.List.of(8L));
        when(scopeService.resolve(9L, 100L, MerchantPermission.ORDER_MANAGE)).thenReturn(scope);
        when(orderMapper.selectOne(any())).thenReturn(order("PENDING_PREPARATION"));
        org.mockito.Mockito.doThrow(new BusinessException("当前员工无权访问该门店"))
                .when(scopeService).requireStoreAccess(scope, 7L);

        assertThrows(BusinessException.class, () -> service.startPreparation(9L, "SO001", 100L, null));

        verify(orderMapper, never()).update(isNull(), any());
        verify(actionMapper, never()).insert(any(OrderFulfillmentAction.class));
    }

    private SalesOrder order(String status) {
        SalesOrder order = new SalesOrder();
        order.setId(1L);
        order.setTenantId(9L);
        order.setStoreId(7L);
        order.setOrderNo("SO001");
        order.setFulfillmentMode("STORE_PICKUP");
        order.setPayStatus("SUCCESS");
        order.setOrderStatus(status);
        order.setDeleted(0);
        return order;
    }
}
