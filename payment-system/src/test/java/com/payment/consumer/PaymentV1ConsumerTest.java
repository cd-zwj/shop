package com.payment.consumer;

import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.PayStatusEnum;
import com.payment.mapper.PointsRuleMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MemberService;
import com.payment.service.ProductInventoryService;
import com.payment.service.WalletRechargeService;
import com.payment.service.WithdrawalService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentV1ConsumerTest {

    @Test
    void handleOrderPaidShouldNotMarkOrderPaidWhenStockDeductionFails() {
        WalletRechargeService walletRechargeService = mock(WalletRechargeService.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper salesOrderItemMapper = mock(SalesOrderItemMapper.class);
        ProductInventoryService productInventoryService = mock(ProductInventoryService.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MemberPointsAccountService memberPointsAccountService = mock(MemberPointsAccountService.class);
        MemberService memberService = mock(MemberService.class);
        PointsRuleMapper pointsRuleMapper = mock(PointsRuleMapper.class);

        PaymentV1Consumer consumer = new PaymentV1Consumer(
                walletRechargeService,
                salesOrderMapper,
                salesOrderItemMapper,
                productInventoryService,
                withdrawalService,
                memberPointsAccountService,
                pointsRuleMapper,
                memberService
        );

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setId(1L);
        salesOrder.setOrderNo("SO001");
        salesOrder.setTenantId(9L);
        salesOrder.setPlatformUserId(100L);
        salesOrder.setOrderStatus(OrderStatusEnum.CREATED.name());
        salesOrder.setPayStatus(PayStatusEnum.WAIT_PAY.name());
        salesOrder.setTotalAmount(new BigDecimal("10.00"));
        salesOrder.setMerchantWalletDeductAmount(BigDecimal.ZERO);
        salesOrder.setDeleted(0);

        SalesOrderItem item = new SalesOrderItem();
        item.setProductId(1L);
        item.setQuantity(2);

        when(salesOrderMapper.selectOne(any())).thenReturn(salesOrder);
        when(salesOrderItemMapper.selectByOrderId(1L)).thenReturn(List.of(item));
        org.mockito.Mockito.doThrow(new RuntimeException("stock failed"))
                .when(productInventoryService).deductStock(9L, 1L, 2, "SO001");

        assertThrows(RuntimeException.class, () -> consumer.handleOrderPaid("{\"bizNo\":\"SO001\"}"));
        verify(salesOrderMapper, never()).updateById(any(SalesOrder.class));
    }
}
