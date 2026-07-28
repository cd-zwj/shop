package com.payment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.entity.SalesOrder;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.MerchantStoreScope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppOrderServiceMerchantScopeTest {

    @Test
    void merchantOrderListShouldPassAssignedStoresToMapper() {
        Fixture fixture = new Fixture();
        MerchantStoreScope scope = new MerchantStoreScope(9L, 3L, false, List.of(7L, 8L));
        when(fixture.scopeService.resolve(9L, 100L, MerchantPermission.ORDER_MANAGE)).thenReturn(scope);
        when(fixture.orderMapper.selectMerchantOrders(
                any(), eq(9L), isNull(), isNull(), isNull(), isNull(), any(), isNull(), any()))
                .thenReturn(new Page<>(1, 10, 0));

        fixture.service.listMerchantOrderViews(
                9L, 100L, 1, 10, null, null, null, null, null, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> storeIds = ArgumentCaptor.forClass(List.class);
        verify(fixture.orderMapper).selectMerchantOrders(
                any(), eq(9L), isNull(), isNull(), isNull(), isNull(), any(), isNull(), storeIds.capture());
        assertTrue(storeIds.getValue().containsAll(List.of(7L, 8L)));
    }

    @Test
    void merchantOrderDetailShouldRejectOtherStoreBeforeLoadingItems() {
        Fixture fixture = new Fixture();
        MerchantStoreScope scope = new MerchantStoreScope(9L, 3L, false, List.of(7L));
        SalesOrder order = new SalesOrder();
        order.setId(5L);
        order.setTenantId(9L);
        order.setStoreId(8L);
        order.setOrderNo("SO001");
        order.setDeleted(0);
        when(fixture.scopeService.resolve(9L, 100L, MerchantPermission.ORDER_MANAGE)).thenReturn(scope);
        when(fixture.orderMapper.selectOne(any())).thenReturn(order);
        doThrow(new BusinessException("当前员工无权访问该门店"))
                .when(fixture.scopeService).requireStoreAccess(scope, 8L);

        assertThrows(BusinessException.class,
                () -> fixture.service.getMerchantOrderDetail(9L, 100L, "SO001"));

        verify(fixture.itemMapper, never()).selectByOrderId(any());
    }

    private static class Fixture {
        private final SalesOrderMapper orderMapper = mock(SalesOrderMapper.class);
        private final SalesOrderItemMapper itemMapper = mock(SalesOrderItemMapper.class);
        private final MerchantStoreScopeService scopeService = mock(MerchantStoreScopeService.class);
        private final AppOrderServiceImpl service = mock(AppOrderServiceImpl.class, CALLS_REAL_METHODS);

        private Fixture() {
            ReflectionTestUtils.setField(service, "salesOrderMapper", orderMapper);
            ReflectionTestUtils.setField(service, "salesOrderItemMapper", itemMapper);
            ReflectionTestUtils.setField(service, "merchantStoreScopeService", scopeService);
        }
    }
}
