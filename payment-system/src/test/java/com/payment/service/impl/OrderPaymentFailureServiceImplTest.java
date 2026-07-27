package com.payment.service.impl;

import com.payment.entity.OrderDiscountSnapshot;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.PayStatusEnum;
import com.payment.mapper.OrderDiscountSnapshotMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.CouponService;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MerchantWalletService;
import com.payment.service.StoreInventoryService;
import com.payment.service.UnifiedWalletService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderPaymentFailureServiceImplTest {

    @Test
    void shouldFailCreatedOrderAndReleaseEveryReservedAsset() {
        Fixture fixture = new Fixture();
        SalesOrder order = fixture.pendingOrder();
        SalesOrderItem item = new SalesOrderItem();
        item.setProductId(300L);
        item.setQuantity(2);
        OrderDiscountSnapshot coupon = new OrderDiscountSnapshot();
        coupon.setDiscountSource("COUPON");
        coupon.setUserCouponId(400L);

        when(fixture.salesOrderMapper.selectByOrderNoForUpdate("SO300")).thenReturn(order);
        when(fixture.salesOrderMapper.failPayment(30L)).thenReturn(1);
        when(fixture.salesOrderItemMapper.selectByOrderId(30L)).thenReturn(List.of(item));
        when(fixture.orderDiscountSnapshotMapper.selectList(any())).thenReturn(List.of(coupon));

        assertTrue(fixture.service.failAndRelease("SO300", "支付渠道返回失败"));

        verify(fixture.storeInventoryService).release(9L, 20L, 300L, 2, "SALES_ORDER", "SO300");
        verify(fixture.couponService).releaseCoupon(400L, 9L, 100L, 30L,
                "SO300", "SO300", "支付渠道返回失败");
        verify(fixture.memberPointsAccountService).releasePointsHold(9L, 100L,
                "ORDER_DEDUCT", "SO300", "支付渠道返回失败");
        verify(fixture.unifiedWalletService).credit(100L, new BigDecimal("3.00"),
                "ORDER_PAYMENT_FAILED_REFUND", "SO300", "支付失败回退");
        verify(fixture.merchantWalletService).credit(9L, 100L, new BigDecimal("2.00"),
                "ORDER_PAYMENT_FAILED_REFUND", "SO300", "支付失败回退");
    }

    @Test
    void shouldNotReleaseAssetsWhenOrderCannotBeClaimedAsFailed() {
        Fixture fixture = new Fixture();
        SalesOrder order = fixture.pendingOrder();
        when(fixture.salesOrderMapper.selectByOrderNoForUpdate("SO300")).thenReturn(order);
        when(fixture.salesOrderMapper.failPayment(30L)).thenReturn(0);

        assertFalse(fixture.service.failAndRelease("SO300", "支付渠道返回失败"));

        verify(fixture.storeInventoryService, never()).release(any(), any(), any(), any(Integer.class), any(), any());
        verify(fixture.couponService, never()).releaseCoupon(any(), any(), any(), any(), any(), any(), any());
        verify(fixture.memberPointsAccountService, never()).releasePointsHold(any(), any(), any(), any(), any());
        verify(fixture.unifiedWalletService, never()).credit(any(), any(), any(), any(), any());
        verify(fixture.merchantWalletService, never()).credit(any(), any(), any(), any(), any(), any());
    }

    private static final class Fixture {
        private final SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        private final SalesOrderItemMapper salesOrderItemMapper = mock(SalesOrderItemMapper.class);
        private final OrderDiscountSnapshotMapper orderDiscountSnapshotMapper = mock(OrderDiscountSnapshotMapper.class);
        private final StoreInventoryService storeInventoryService = mock(StoreInventoryService.class);
        private final CouponService couponService = mock(CouponService.class);
        private final MemberPointsAccountService memberPointsAccountService = mock(MemberPointsAccountService.class);
        private final UnifiedWalletService unifiedWalletService = mock(UnifiedWalletService.class);
        private final MerchantWalletService merchantWalletService = mock(MerchantWalletService.class);
        private final OrderPaymentFailureServiceImpl service = new OrderPaymentFailureServiceImpl(
                salesOrderMapper,
                salesOrderItemMapper,
                orderDiscountSnapshotMapper,
                storeInventoryService,
                couponService,
                memberPointsAccountService,
                unifiedWalletService,
                merchantWalletService
        );

        private SalesOrder pendingOrder() {
            SalesOrder order = new SalesOrder();
            order.setId(30L);
            order.setOrderNo("SO300");
            order.setTenantId(9L);
            order.setPlatformUserId(100L);
            order.setStoreId(20L);
            order.setFulfillmentMode("STORE_PICKUP");
            order.setOrderStatus(OrderStatusEnum.CREATED.name());
            order.setPayStatus(PayStatusEnum.WAIT_PAY.name());
            order.setPointsDeductAmount(new BigDecimal("1.00"));
            order.setUnifiedWalletDeductAmount(new BigDecimal("3.00"));
            order.setMerchantWalletDeductAmount(new BigDecimal("2.00"));
            return order;
        }
    }
}
