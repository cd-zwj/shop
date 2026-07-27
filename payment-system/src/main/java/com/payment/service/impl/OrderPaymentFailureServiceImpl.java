package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.OrderDiscountSnapshot;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.DiscountSourceEnum;
import com.payment.mapper.OrderDiscountSnapshotMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.CouponService;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MerchantWalletService;
import com.payment.service.OrderPaymentFailureService;
import com.payment.service.StoreInventoryService;
import com.payment.service.UnifiedWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPaymentFailureServiceImpl implements OrderPaymentFailureService {

    private static final String ORDER_BIZ_TYPE = "SALES_ORDER";
    private static final String POINTS_BIZ_TYPE = "ORDER_DEDUCT";
    private static final String WALLET_REFUND_BIZ_TYPE = "ORDER_PAYMENT_FAILED_REFUND";

    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final OrderDiscountSnapshotMapper orderDiscountSnapshotMapper;
    private final StoreInventoryService storeInventoryService;
    private final CouponService couponService;
    private final MemberPointsAccountService memberPointsAccountService;
    private final UnifiedWalletService unifiedWalletService;
    private final MerchantWalletService merchantWalletService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean failAndRelease(String orderNo, String reason) {
        SalesOrder order = salesOrderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null || salesOrderMapper.failPayment(order.getId()) != 1) {
            return false;
        }

        releaseInventory(order);
        releaseCoupons(order, reason);
        releasePoints(order, reason);
        refundWalletDeductions(order);
        return true;
    }

    private void releaseInventory(SalesOrder order) {
        if (!"STORE_PICKUP".equals(order.getFulfillmentMode()) || order.getStoreId() == null) {
            return;
        }
        List<SalesOrderItem> items = salesOrderItemMapper.selectByOrderId(order.getId());
        for (SalesOrderItem item : items) {
            storeInventoryService.release(
                    order.getTenantId(), order.getStoreId(), item.getProductId(), item.getQuantity(),
                    ORDER_BIZ_TYPE, order.getOrderNo());
        }
    }

    private void releaseCoupons(SalesOrder order, String reason) {
        List<OrderDiscountSnapshot> snapshots = orderDiscountSnapshotMapper.selectList(
                new LambdaQueryWrapper<OrderDiscountSnapshot>()
                        .eq(OrderDiscountSnapshot::getOrderNo, order.getOrderNo()));
        for (OrderDiscountSnapshot snapshot : snapshots) {
            if (DiscountSourceEnum.COUPON.name().equals(snapshot.getDiscountSource())
                    && snapshot.getUserCouponId() != null) {
                couponService.releaseCoupon(
                        snapshot.getUserCouponId(), order.getTenantId(), order.getPlatformUserId(),
                        order.getId(), order.getOrderNo(), order.getOrderNo(), reason);
            }
        }
    }

    private void releasePoints(SalesOrder order, String reason) {
        if (positive(order.getPointsDeductAmount())) {
            memberPointsAccountService.releasePointsHold(
                    order.getTenantId(), order.getPlatformUserId(), POINTS_BIZ_TYPE, order.getOrderNo(), reason);
        }
    }

    private void refundWalletDeductions(SalesOrder order) {
        if (positive(order.getUnifiedWalletDeductAmount())) {
            unifiedWalletService.credit(
                    order.getPlatformUserId(), order.getUnifiedWalletDeductAmount(),
                    WALLET_REFUND_BIZ_TYPE, order.getOrderNo(), "支付失败回退");
        }
        if (positive(order.getMerchantWalletDeductAmount())) {
            merchantWalletService.credit(
                    order.getTenantId(), order.getPlatformUserId(), order.getMerchantWalletDeductAmount(),
                    WALLET_REFUND_BIZ_TYPE, order.getOrderNo(), "支付失败回退");
        }
    }

    private boolean positive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
