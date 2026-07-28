package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.entity.RefundApplication;
import com.payment.entity.SalesOrder;
import com.payment.mapper.AfterSaleActionMapper;
import com.payment.mapper.RefundApplicationMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.MerchantStoreScope;
import com.payment.service.RefundService;
import com.payment.service.StoreInventoryService;
import com.payment.service.UserNotificationService;
import com.payment.service.delivery.OrderDeliveryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MerchantRefundStoreScopeTest {

    @Test
    void auditShouldRejectRefundFromAnotherAssignedStoreBeforeMutation() {
        RefundApplicationMapper refundMapper = mock(RefundApplicationMapper.class);
        SalesOrderMapper orderMapper = mock(SalesOrderMapper.class);
        MerchantStoreScopeService scopeService = mock(MerchantStoreScopeService.class);
        RefundApplication refund = new RefundApplication();
        refund.setId(5L);
        refund.setTenantId(9L);
        refund.setOrderNo("SO001");
        refund.setRefundStatus("PENDING");
        SalesOrder order = new SalesOrder();
        order.setTenantId(9L);
        order.setStoreId(8L);
        MerchantStoreScope scope = new MerchantStoreScope(9L, 3L, false, List.of(7L));
        when(scopeService.resolve(9L, 100L, MerchantPermission.REFUND_MANAGE)).thenReturn(scope);
        when(refundMapper.selectById(5L)).thenReturn(refund);
        when(orderMapper.selectOne(any())).thenReturn(order);
        doThrow(new BusinessException("当前员工无权访问该门店"))
                .when(scopeService).requireStoreAccess(scope, 8L);
        RefundApplicationServiceImpl service = new RefundApplicationServiceImpl(
                refundMapper, orderMapper, mock(SalesOrderItemMapper.class), mock(UserNotificationService.class),
                mock(OrderDeliveryService.class), mock(RefundService.class), mock(StoreInventoryService.class),
                mock(AfterSaleActionMapper.class), scopeService);

        assertThrows(BusinessException.class,
                () -> service.auditMerchantRefund(9L, 5L, 100L, true, null));

        verify(refundMapper, never()).updateById(any(RefundApplication.class));
    }
}
