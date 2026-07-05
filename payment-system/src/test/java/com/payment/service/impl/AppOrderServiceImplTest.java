package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.dto.AppCreateOrderDTO;
import com.payment.dto.AppCreateOrderItemDTO;
import com.payment.dto.OrderPaymentVO;
import com.payment.dto.PayResponseDTO;
import com.payment.dto.WalletAccountVO;
import com.payment.dto.pricing.OrderPricingResultVO;
import com.payment.entity.PaymentBill;
import com.payment.entity.Product;
import com.payment.entity.ProductStock;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.entity.TenantEmployee;
import com.payment.entity.TenantMember;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.PayStatusEnum;
import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.mapper.PointsRuleMapper;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.mapper.TenantEmployeeMapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.mapper.OrderDiscountSnapshotMapper;
import com.payment.service.CouponService;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MerchantWalletService;
import com.payment.service.OrderPricingService;
import com.payment.service.PaymentBillV1Service;
import com.payment.service.PromotionService;
import com.payment.service.UnifiedWalletService;
import com.payment.service.UserBehaviorLogService;
import com.payment.service.WithdrawalService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppOrderServiceImplTest {

    @Test
    void createOrderShouldRecalculateAmountAndPersistOrderItems() {
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper salesOrderItemMapper = mock(SalesOrderItemMapper.class);
        TenantEmployeeMapper tenantEmployeeMapper = mock(TenantEmployeeMapper.class);
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        UnifiedWalletService unifiedWalletService = mock(UnifiedWalletService.class);
        MerchantWalletService merchantWalletService = mock(MerchantWalletService.class);
        PaymentBillV1Service paymentBillV1Service = mock(PaymentBillV1Service.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MemberPointsAccountService memberPointsAccountService = mock(MemberPointsAccountService.class);
        PointsRuleMapper pointsRuleMapper = mock(PointsRuleMapper.class);
        OrderPricingService orderPricingService = mock(OrderPricingService.class);
        CouponService couponService = mock(CouponService.class);
        PromotionService promotionService = mock(PromotionService.class);
        OrderDiscountSnapshotMapper orderDiscountSnapshotMapper = mock(OrderDiscountSnapshotMapper.class);
        UserBehaviorLogService userBehaviorLogService = mock(UserBehaviorLogService.class);
        com.payment.service.delivery.OrderDeliveryService orderDeliveryService = mock(com.payment.service.delivery.OrderDeliveryService.class);

        AppOrderServiceImpl service = new AppOrderServiceImpl(
                salesOrderMapper,
                salesOrderItemMapper,
                tenantEmployeeMapper,
                tenantMemberMapper,
                productMapper,
                unifiedWalletService,
                merchantWalletService,
                paymentBillV1Service,
                withdrawalService,
                memberPointsAccountService,
                pointsRuleMapper,
                orderPricingService,
                couponService,
                promotionService,
                orderDiscountSnapshotMapper,
                userBehaviorLogService,
                orderDeliveryService
        );

        when(tenantMemberMapper.selectOne(any())).thenReturn(new TenantMember());
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(
                buildProduct(1L, 9L, "可乐", "3.50"),
                buildProduct(2L, 9L, "雪碧", "4.00")
        ));
        when(productMapper.selectStockByTenantAndProductIds(eq(9L), any())).thenReturn(List.of(
                buildStock(1L, 10),
                buildStock(2L, 10)
        ));
        when(unifiedWalletService.getWallet(100L)).thenReturn(buildWallet("0.00"));
        when(merchantWalletService.getWallet(9L, 100L)).thenReturn(buildWallet("0.00"));
        when(orderPricingService.calculate(any())).thenReturn(buildPricingResult("11.00", "11.00"));

        doAnswer(invocation -> {
            SalesOrder salesOrder = invocation.getArgument(0);
            salesOrder.setId(88L);
            return 1;
        }).when(salesOrderMapper).insert(any(SalesOrder.class));

        PaymentBill paymentBill = new PaymentBill();
        paymentBill.setBillNo("PB001");
        when(paymentBillV1Service.createBill(any(), any(), any(), any(), any(), any())).thenReturn(paymentBill);

        PayResponseDTO payResponseDTO = new PayResponseDTO();
        payResponseDTO.setPayUrl("https://pay.local/PB001");
        when(paymentBillV1Service.createExternalPayment(paymentBill)).thenReturn(payResponseDTO);

        AppCreateOrderDTO dto = new AppCreateOrderDTO();
        dto.setTenantId(9L);
        dto.setTotalAmount(new BigDecimal("1.00"));
        dto.setWalletStrategy(com.payment.enums.WalletStrategyEnum.NO_WALLET);
        dto.setPaymentChannelCode(PaymentChannelCodeEnum.ALIPAY_PAGE);
        dto.setItems(List.of(
                buildItem(1L, 2),
                buildItem(2L, 1)
        ));

        OrderPaymentVO result = service.createOrder(100L, dto);

        ArgumentCaptor<SalesOrder> orderCaptor = ArgumentCaptor.forClass(SalesOrder.class);
        verify(salesOrderMapper).insert(orderCaptor.capture());
        SalesOrder savedOrder = orderCaptor.getValue();
        assertEquals(new BigDecimal("11.00"), savedOrder.getTotalAmount());
        assertEquals(new BigDecimal("11.00"), savedOrder.getPayableAmount());
        assertEquals("可乐等2件商品", savedOrder.getSubject());
        assertEquals(66L, savedOrder.getStoreId());

        ArgumentCaptor<List<SalesOrderItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
        verify(salesOrderItemMapper, times(1)).insertBatch(itemCaptor.capture());
        List<SalesOrderItem> items = itemCaptor.getValue();
        assertEquals(2, items.size());
        assertEquals(88L, items.get(0).getOrderId());
        assertEquals(new BigDecimal("7.00"), items.get(0).getSubtotal());
        assertEquals(new BigDecimal("4.00"), items.get(1).getSubtotal());

        assertEquals("PB001", result.getPaymentBillNo());
        assertEquals("https://pay.local/PB001", result.getExternalPayUrl());
    }

    @Test
    void createOrderShouldSupportPureWalletPaymentAndStillWriteItems() {
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper salesOrderItemMapper = mock(SalesOrderItemMapper.class);
        TenantEmployeeMapper tenantEmployeeMapper = mock(TenantEmployeeMapper.class);
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        UnifiedWalletService unifiedWalletService = mock(UnifiedWalletService.class);
        MerchantWalletService merchantWalletService = mock(MerchantWalletService.class);
        PaymentBillV1Service paymentBillV1Service = mock(PaymentBillV1Service.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MemberPointsAccountService memberPointsAccountService = mock(MemberPointsAccountService.class);
        PointsRuleMapper pointsRuleMapper = mock(PointsRuleMapper.class);
        OrderPricingService orderPricingService = mock(OrderPricingService.class);
        CouponService couponService = mock(CouponService.class);
        PromotionService promotionService = mock(PromotionService.class);
        OrderDiscountSnapshotMapper orderDiscountSnapshotMapper = mock(OrderDiscountSnapshotMapper.class);
        UserBehaviorLogService userBehaviorLogService = mock(UserBehaviorLogService.class);
        com.payment.service.delivery.OrderDeliveryService orderDeliveryService = mock(com.payment.service.delivery.OrderDeliveryService.class);

        AppOrderServiceImpl service = new AppOrderServiceImpl(
                salesOrderMapper,
                salesOrderItemMapper,
                tenantEmployeeMapper,
                tenantMemberMapper,
                productMapper,
                unifiedWalletService,
                merchantWalletService,
                paymentBillV1Service,
                withdrawalService,
                memberPointsAccountService,
                pointsRuleMapper,
                orderPricingService,
                couponService,
                promotionService,
                orderDiscountSnapshotMapper,
                userBehaviorLogService,
                orderDeliveryService
        );

        when(tenantMemberMapper.selectOne(any())).thenReturn(new TenantMember());
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(buildProduct(3L, 9L, "奶茶", "12.00")));
        when(productMapper.selectStockByTenantAndProductIds(eq(9L), any())).thenReturn(List.of(buildStock(3L, 5)));
        when(unifiedWalletService.getWallet(100L)).thenReturn(buildWallet("20.00"));
        when(merchantWalletService.getWallet(9L, 100L)).thenReturn(buildWallet("0.00"));
        when(orderPricingService.calculate(any())).thenReturn(buildPricingResult("12.00", "12.00"));

        doAnswer(invocation -> {
            SalesOrder salesOrder = invocation.getArgument(0);
            salesOrder.setId(99L);
            return 1;
        }).when(salesOrderMapper).insert(any(SalesOrder.class));

        AppCreateOrderDTO dto = new AppCreateOrderDTO();
        dto.setTenantId(9L);
        dto.setWalletStrategy(com.payment.enums.WalletStrategyEnum.UNIFIED_ONLY);
        dto.setAllowExternalPayFallback(false);
        dto.setItems(List.of(buildItem(3L, 1)));

        OrderPaymentVO result = service.createOrder(100L, dto);

        verify(unifiedWalletService).debit(100L, new BigDecimal("12.00"), "SALES_ORDER", result.getOrderNo(), "订单消费扣减");
        verify(paymentBillV1Service, never()).createBill(any(), any(), any(), any(), any(), any());
        verify(salesOrderItemMapper, times(1)).insertBatch(any());
        assertNull(result.getPaymentBillNo());
        assertEquals("PAID", result.getOrderStatus());
        // 回归 H1：钱包支付分支(无外部回调)也必须入队交付事件,否则用户付钱永远收不到商品
        verify(orderDeliveryService, times(1)).enqueueDelivery(result.getOrderNo());
    }

    @Test
    void createOrderShouldRejectInsufficientStockBeforePricing() {
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper salesOrderItemMapper = mock(SalesOrderItemMapper.class);
        TenantEmployeeMapper tenantEmployeeMapper = mock(TenantEmployeeMapper.class);
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        UnifiedWalletService unifiedWalletService = mock(UnifiedWalletService.class);
        MerchantWalletService merchantWalletService = mock(MerchantWalletService.class);
        PaymentBillV1Service paymentBillV1Service = mock(PaymentBillV1Service.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MemberPointsAccountService memberPointsAccountService = mock(MemberPointsAccountService.class);
        PointsRuleMapper pointsRuleMapper = mock(PointsRuleMapper.class);
        OrderPricingService orderPricingService = mock(OrderPricingService.class);
        CouponService couponService = mock(CouponService.class);
        PromotionService promotionService = mock(PromotionService.class);
        OrderDiscountSnapshotMapper orderDiscountSnapshotMapper = mock(OrderDiscountSnapshotMapper.class);
        UserBehaviorLogService userBehaviorLogService = mock(UserBehaviorLogService.class);
        com.payment.service.delivery.OrderDeliveryService orderDeliveryService = mock(com.payment.service.delivery.OrderDeliveryService.class);

        AppOrderServiceImpl service = new AppOrderServiceImpl(
                salesOrderMapper,
                salesOrderItemMapper,
                tenantEmployeeMapper,
                tenantMemberMapper,
                productMapper,
                unifiedWalletService,
                merchantWalletService,
                paymentBillV1Service,
                withdrawalService,
                memberPointsAccountService,
                pointsRuleMapper,
                orderPricingService,
                couponService,
                promotionService,
                orderDiscountSnapshotMapper,
                userBehaviorLogService,
                orderDeliveryService
        );

        when(productMapper.selectBatchIds(any())).thenReturn(List.of(buildProduct(1L, 9L, "可乐", "3.50")));
        when(productMapper.selectStockByTenantAndProductIds(eq(9L), any())).thenReturn(List.of(buildStock(1L, 1)));

        AppCreateOrderDTO dto = new AppCreateOrderDTO();
        dto.setTenantId(9L);
        dto.setWalletStrategy(com.payment.enums.WalletStrategyEnum.NO_WALLET);
        dto.setItems(List.of(buildItem(1L, 2)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createOrder(100L, dto));

        assertEquals("商品库存不足, productId=1", exception.getMessage());
        verify(salesOrderMapper, never()).insert(any(SalesOrder.class));
        verify(orderPricingService, never()).calculate(any());
    }

    @Test
    void createOrderShouldRejectStaleClientPriceBeforePricing() {
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper salesOrderItemMapper = mock(SalesOrderItemMapper.class);
        TenantEmployeeMapper tenantEmployeeMapper = mock(TenantEmployeeMapper.class);
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        UnifiedWalletService unifiedWalletService = mock(UnifiedWalletService.class);
        MerchantWalletService merchantWalletService = mock(MerchantWalletService.class);
        PaymentBillV1Service paymentBillV1Service = mock(PaymentBillV1Service.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MemberPointsAccountService memberPointsAccountService = mock(MemberPointsAccountService.class);
        PointsRuleMapper pointsRuleMapper = mock(PointsRuleMapper.class);
        OrderPricingService orderPricingService = mock(OrderPricingService.class);
        CouponService couponService = mock(CouponService.class);
        PromotionService promotionService = mock(PromotionService.class);
        OrderDiscountSnapshotMapper orderDiscountSnapshotMapper = mock(OrderDiscountSnapshotMapper.class);
        UserBehaviorLogService userBehaviorLogService = mock(UserBehaviorLogService.class);
        com.payment.service.delivery.OrderDeliveryService orderDeliveryService = mock(com.payment.service.delivery.OrderDeliveryService.class);

        AppOrderServiceImpl service = new AppOrderServiceImpl(
                salesOrderMapper,
                salesOrderItemMapper,
                tenantEmployeeMapper,
                tenantMemberMapper,
                productMapper,
                unifiedWalletService,
                merchantWalletService,
                paymentBillV1Service,
                withdrawalService,
                memberPointsAccountService,
                pointsRuleMapper,
                orderPricingService,
                couponService,
                promotionService,
                orderDiscountSnapshotMapper,
                userBehaviorLogService,
                orderDeliveryService
        );

        when(productMapper.selectBatchIds(any())).thenReturn(List.of(buildProduct(1L, 9L, "可乐", "3.50")));
        when(productMapper.selectStockByTenantAndProductIds(eq(9L), any())).thenReturn(List.of(buildStock(1L, 10)));

        AppCreateOrderDTO dto = new AppCreateOrderDTO();
        dto.setTenantId(9L);
        dto.setWalletStrategy(com.payment.enums.WalletStrategyEnum.NO_WALLET);
        dto.setItems(List.of(buildItemWithPrice(1L, 1, "300")));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createOrder(100L, dto));

        assertEquals("商品价格已变化, productId=1", exception.getMessage());
        verify(salesOrderMapper, never()).insert(any(SalesOrder.class));
        verify(orderPricingService, never()).calculate(any());
    }

    @Test
    void createOrderShouldRejectProductFromOtherTenant() {
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper salesOrderItemMapper = mock(SalesOrderItemMapper.class);
        TenantEmployeeMapper tenantEmployeeMapper = mock(TenantEmployeeMapper.class);
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        UnifiedWalletService unifiedWalletService = mock(UnifiedWalletService.class);
        MerchantWalletService merchantWalletService = mock(MerchantWalletService.class);
        PaymentBillV1Service paymentBillV1Service = mock(PaymentBillV1Service.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MemberPointsAccountService memberPointsAccountService = mock(MemberPointsAccountService.class);
        PointsRuleMapper pointsRuleMapper = mock(PointsRuleMapper.class);
        OrderPricingService orderPricingService = mock(OrderPricingService.class);
        CouponService couponService = mock(CouponService.class);
        PromotionService promotionService = mock(PromotionService.class);
        OrderDiscountSnapshotMapper orderDiscountSnapshotMapper = mock(OrderDiscountSnapshotMapper.class);
        UserBehaviorLogService userBehaviorLogService = mock(UserBehaviorLogService.class);
        com.payment.service.delivery.OrderDeliveryService orderDeliveryService = mock(com.payment.service.delivery.OrderDeliveryService.class);

        AppOrderServiceImpl service = new AppOrderServiceImpl(
                salesOrderMapper,
                salesOrderItemMapper,
                tenantEmployeeMapper,
                tenantMemberMapper,
                productMapper,
                unifiedWalletService,
                merchantWalletService,
                paymentBillV1Service,
                withdrawalService,
                memberPointsAccountService,
                pointsRuleMapper,
                orderPricingService,
                couponService,
                promotionService,
                orderDiscountSnapshotMapper,
                userBehaviorLogService,
                orderDeliveryService
        );

        when(productMapper.selectBatchIds(any())).thenReturn(List.of(buildProduct(1L, 10L, "别家商品", "9.90")));

        AppCreateOrderDTO dto = new AppCreateOrderDTO();
        dto.setTenantId(9L);
        dto.setWalletStrategy(com.payment.enums.WalletStrategyEnum.NO_WALLET);
        dto.setItems(List.of(buildItem(1L, 1)));

        assertThrows(BusinessException.class, () -> service.createOrder(100L, dto));
        verify(salesOrderMapper, never()).insert(any(SalesOrder.class));
        verify(orderPricingService, never()).calculate(any());
    }

    @Test
    void getMerchantOrderDetailShouldRequireTenantEmployeeRelation() {
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper salesOrderItemMapper = mock(SalesOrderItemMapper.class);
        TenantEmployeeMapper tenantEmployeeMapper = mock(TenantEmployeeMapper.class);
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        UnifiedWalletService unifiedWalletService = mock(UnifiedWalletService.class);
        MerchantWalletService merchantWalletService = mock(MerchantWalletService.class);
        PaymentBillV1Service paymentBillV1Service = mock(PaymentBillV1Service.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MemberPointsAccountService memberPointsAccountService = mock(MemberPointsAccountService.class);
        PointsRuleMapper pointsRuleMapper = mock(PointsRuleMapper.class);
        OrderPricingService orderPricingService = mock(OrderPricingService.class);
        CouponService couponService = mock(CouponService.class);
        PromotionService promotionService = mock(PromotionService.class);
        OrderDiscountSnapshotMapper orderDiscountSnapshotMapper = mock(OrderDiscountSnapshotMapper.class);
        UserBehaviorLogService userBehaviorLogService = mock(UserBehaviorLogService.class);
        com.payment.service.delivery.OrderDeliveryService orderDeliveryService = mock(com.payment.service.delivery.OrderDeliveryService.class);

        AppOrderServiceImpl service = new AppOrderServiceImpl(
                salesOrderMapper,
                salesOrderItemMapper,
                tenantEmployeeMapper,
                tenantMemberMapper,
                productMapper,
                unifiedWalletService,
                merchantWalletService,
                paymentBillV1Service,
                withdrawalService,
                memberPointsAccountService,
                pointsRuleMapper,
                orderPricingService,
                couponService,
                promotionService,
                orderDiscountSnapshotMapper,
                userBehaviorLogService,
                orderDeliveryService
        );

        when(tenantEmployeeMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.getMerchantOrderDetail(9L, 100L, "SO001"));
    }

    @Test
    void getOrderDetailShouldExposeLatestPaymentBillFailureContext() {
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper salesOrderItemMapper = mock(SalesOrderItemMapper.class);
        TenantEmployeeMapper tenantEmployeeMapper = mock(TenantEmployeeMapper.class);
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        UnifiedWalletService unifiedWalletService = mock(UnifiedWalletService.class);
        MerchantWalletService merchantWalletService = mock(MerchantWalletService.class);
        PaymentBillV1Service paymentBillV1Service = mock(PaymentBillV1Service.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MemberPointsAccountService memberPointsAccountService = mock(MemberPointsAccountService.class);
        PointsRuleMapper pointsRuleMapper = mock(PointsRuleMapper.class);
        OrderPricingService orderPricingService = mock(OrderPricingService.class);
        CouponService couponService = mock(CouponService.class);
        PromotionService promotionService = mock(PromotionService.class);
        OrderDiscountSnapshotMapper orderDiscountSnapshotMapper = mock(OrderDiscountSnapshotMapper.class);
        UserBehaviorLogService userBehaviorLogService = mock(UserBehaviorLogService.class);
        com.payment.service.delivery.OrderDeliveryService orderDeliveryService = mock(com.payment.service.delivery.OrderDeliveryService.class);

        AppOrderServiceImpl service = new AppOrderServiceImpl(
                salesOrderMapper,
                salesOrderItemMapper,
                tenantEmployeeMapper,
                tenantMemberMapper,
                productMapper,
                unifiedWalletService,
                merchantWalletService,
                paymentBillV1Service,
                withdrawalService,
                memberPointsAccountService,
                pointsRuleMapper,
                orderPricingService,
                couponService,
                promotionService,
                orderDiscountSnapshotMapper,
                userBehaviorLogService,
                orderDeliveryService
        );

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setId(103L);
        salesOrder.setOrderNo("SO1003");
        salesOrder.setTenantId(9L);
        salesOrder.setPlatformUserId(100L);
        salesOrder.setOrderStatus(OrderStatusEnum.CREATED.name());
        salesOrder.setPayStatus(PayStatusEnum.FAILED.name());
        salesOrder.setTotalAmount(new BigDecimal("20.00"));

        PaymentBill failedBill = new PaymentBill();
        failedBill.setBillNo("PB_FAILED");
        failedBill.setPayStatus(PayStatusEnum.FAILED.name());
        failedBill.setStatusRemark("渠道返回：余额不足");
        failedBill.setExpireTime(LocalDateTime.of(2026, 7, 5, 10, 30));

        when(salesOrderMapper.selectOne(any())).thenReturn(salesOrder);
        when(salesOrderItemMapper.selectByOrderId(103L)).thenReturn(List.of());
        when(paymentBillV1Service.getLatestByBizTypeAndBizNo("SALES_ORDER", "SO1003"))
                .thenReturn(failedBill);

        com.payment.dto.SalesOrderDetailVO detail = service.getOrderDetail(100L, "SO1003");

        assertEquals("PB_FAILED", detail.getPaymentBillNo());
        assertEquals("FAILED", detail.getPaymentBillStatus());
        assertEquals("渠道返回：余额不足", detail.getPaymentBillStatusRemark());
        assertEquals(LocalDateTime.of(2026, 7, 5, 10, 30), detail.getPaymentBillExpireTime());
    }

    @Test
    void repayOrderShouldReuseExistingActivePaymentBillBeforeCreatingNewOne() {
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper salesOrderItemMapper = mock(SalesOrderItemMapper.class);
        TenantEmployeeMapper tenantEmployeeMapper = mock(TenantEmployeeMapper.class);
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        UnifiedWalletService unifiedWalletService = mock(UnifiedWalletService.class);
        MerchantWalletService merchantWalletService = mock(MerchantWalletService.class);
        PaymentBillV1Service paymentBillV1Service = mock(PaymentBillV1Service.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MemberPointsAccountService memberPointsAccountService = mock(MemberPointsAccountService.class);
        PointsRuleMapper pointsRuleMapper = mock(PointsRuleMapper.class);
        OrderPricingService orderPricingService = mock(OrderPricingService.class);
        CouponService couponService = mock(CouponService.class);
        PromotionService promotionService = mock(PromotionService.class);
        OrderDiscountSnapshotMapper orderDiscountSnapshotMapper = mock(OrderDiscountSnapshotMapper.class);
        UserBehaviorLogService userBehaviorLogService = mock(UserBehaviorLogService.class);
        com.payment.service.delivery.OrderDeliveryService orderDeliveryService = mock(com.payment.service.delivery.OrderDeliveryService.class);

        AppOrderServiceImpl service = new AppOrderServiceImpl(
                salesOrderMapper,
                salesOrderItemMapper,
                tenantEmployeeMapper,
                tenantMemberMapper,
                productMapper,
                unifiedWalletService,
                merchantWalletService,
                paymentBillV1Service,
                withdrawalService,
                memberPointsAccountService,
                pointsRuleMapper,
                orderPricingService,
                couponService,
                promotionService,
                orderDiscountSnapshotMapper,
                userBehaviorLogService,
                orderDeliveryService
        );

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setId(101L);
        salesOrder.setOrderNo("SO1001");
        salesOrder.setTenantId(9L);
        salesOrder.setPlatformUserId(100L);
        salesOrder.setOrderStatus(OrderStatusEnum.CREATED.name());
        salesOrder.setPayStatus(PayStatusEnum.WAIT_PAY.name());
        salesOrder.setTotalAmount(new BigDecimal("11.00"));
        salesOrder.setUnifiedWalletDeductAmount(BigDecimal.ZERO);
        salesOrder.setMerchantWalletDeductAmount(BigDecimal.ZERO);
        salesOrder.setExternalPayAmount(new BigDecimal("11.00"));

        PaymentBill closedBill = new PaymentBill();
        closedBill.setBillNo("PB_OLD");
        closedBill.setBizType("SALES_ORDER");
        closedBill.setBizNo("SO1001");
        closedBill.setPayStatus(PayStatusEnum.CLOSED.name());

        PaymentBill activeBill = new PaymentBill();
        activeBill.setBillNo("PB_ACTIVE");
        activeBill.setBizType("SALES_ORDER");
        activeBill.setBizNo("SO1001");
        activeBill.setPayStatus(PayStatusEnum.WAIT_PAY.name());
        activeBill.setExpireTime(LocalDateTime.now().plusMinutes(10));

        PayResponseDTO payResponseDTO = new PayResponseDTO();
        payResponseDTO.setPayUrl("https://pay.local/PB_ACTIVE");

        when(salesOrderMapper.selectOne(any())).thenReturn(salesOrder);
        when(paymentBillV1Service.listByBizTypeAndBizNo("SALES_ORDER", "SO1001"))
                .thenReturn(List.of(activeBill, closedBill));
        when(paymentBillV1Service.syncBillStatus("PB_ACTIVE")).thenReturn(activeBill);
        when(paymentBillV1Service.createExternalPayment(activeBill)).thenReturn(payResponseDTO);

        OrderPaymentVO result = service.repayOrder(100L, "SO1001", PaymentChannelCodeEnum.ALIPAY_PAGE);

        verify(paymentBillV1Service, never()).createBill(any(), any(), any(), any(), any(), any());
        assertEquals("PB_ACTIVE", result.getPaymentBillNo());
        assertEquals("https://pay.local/PB_ACTIVE", result.getExternalPayUrl());
        assertEquals(Boolean.TRUE, result.getReusedPaymentBill());
    }

    @Test
    void repayOrderShouldCreateNewPaymentBillWhenNoReusableBillExists() {
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        SalesOrderItemMapper salesOrderItemMapper = mock(SalesOrderItemMapper.class);
        TenantEmployeeMapper tenantEmployeeMapper = mock(TenantEmployeeMapper.class);
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        UnifiedWalletService unifiedWalletService = mock(UnifiedWalletService.class);
        MerchantWalletService merchantWalletService = mock(MerchantWalletService.class);
        PaymentBillV1Service paymentBillV1Service = mock(PaymentBillV1Service.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        MemberPointsAccountService memberPointsAccountService = mock(MemberPointsAccountService.class);
        PointsRuleMapper pointsRuleMapper = mock(PointsRuleMapper.class);
        OrderPricingService orderPricingService = mock(OrderPricingService.class);
        CouponService couponService = mock(CouponService.class);
        PromotionService promotionService = mock(PromotionService.class);
        OrderDiscountSnapshotMapper orderDiscountSnapshotMapper = mock(OrderDiscountSnapshotMapper.class);
        UserBehaviorLogService userBehaviorLogService = mock(UserBehaviorLogService.class);
        com.payment.service.delivery.OrderDeliveryService orderDeliveryService = mock(com.payment.service.delivery.OrderDeliveryService.class);

        AppOrderServiceImpl service = new AppOrderServiceImpl(
                salesOrderMapper,
                salesOrderItemMapper,
                tenantEmployeeMapper,
                tenantMemberMapper,
                productMapper,
                unifiedWalletService,
                merchantWalletService,
                paymentBillV1Service,
                withdrawalService,
                memberPointsAccountService,
                pointsRuleMapper,
                orderPricingService,
                couponService,
                promotionService,
                orderDiscountSnapshotMapper,
                userBehaviorLogService,
                orderDeliveryService
        );

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setId(102L);
        salesOrder.setOrderNo("SO1002");
        salesOrder.setTenantId(9L);
        salesOrder.setPlatformUserId(100L);
        salesOrder.setOrderStatus(OrderStatusEnum.CREATED.name());
        salesOrder.setPayStatus(PayStatusEnum.WAIT_PAY.name());
        salesOrder.setTotalAmount(new BigDecimal("20.00"));
        salesOrder.setUnifiedWalletDeductAmount(BigDecimal.ZERO);
        salesOrder.setMerchantWalletDeductAmount(BigDecimal.ZERO);
        salesOrder.setExternalPayAmount(new BigDecimal("20.00"));

        PaymentBill expiredBill = new PaymentBill();
        expiredBill.setBillNo("PB_EXPIRED");
        expiredBill.setBizType("SALES_ORDER");
        expiredBill.setBizNo("SO1002");
        expiredBill.setPayStatus(PayStatusEnum.WAIT_PAY.name());
        expiredBill.setExpireTime(LocalDateTime.now().minusMinutes(1));

        PaymentBill newBill = new PaymentBill();
        newBill.setBillNo("PB_NEW");

        PayResponseDTO payResponseDTO = new PayResponseDTO();
        payResponseDTO.setPayUrl("https://pay.local/PB_NEW");

        when(salesOrderMapper.selectOne(any())).thenReturn(salesOrder);
        when(paymentBillV1Service.listByBizTypeAndBizNo("SALES_ORDER", "SO1002"))
                .thenReturn(List.of(expiredBill));
        when(paymentBillV1Service.syncBillStatus("PB_EXPIRED")).thenReturn(expiredBill);
        when(paymentBillV1Service.createBill(
                eq("SALES_ORDER"),
                eq("SO1002"),
                eq(9L),
                eq(100L),
                eq(new BigDecimal("20.00")),
                eq(PaymentChannelCodeEnum.ALIPAY_PAGE)
        )).thenReturn(newBill);
        when(paymentBillV1Service.createExternalPayment(newBill)).thenReturn(payResponseDTO);

        OrderPaymentVO result = service.repayOrder(100L, "SO1002", PaymentChannelCodeEnum.ALIPAY_PAGE);

        verify(paymentBillV1Service).markBillClosed(eq("PB_EXPIRED"), any());
        verify(paymentBillV1Service).createBill(
                eq("SALES_ORDER"),
                eq("SO1002"),
                eq(9L),
                eq(100L),
                eq(new BigDecimal("20.00")),
                eq(PaymentChannelCodeEnum.ALIPAY_PAGE)
        );
        assertEquals("PB_NEW", result.getPaymentBillNo());
        assertEquals("https://pay.local/PB_NEW", result.getExternalPayUrl());
        assertEquals(Boolean.FALSE, result.getReusedPaymentBill());
    }

    private Product buildProduct(Long id, Long tenantId, String name, String price) {
        Product product = new Product();
        product.setId(id);
        product.setTenantId(tenantId);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setStoreId(66L);
        product.setStatus(1);
        product.setDeleted(0);
        return product;
    }

    private WalletAccountVO buildWallet(String balance) {
        WalletAccountVO wallet = new WalletAccountVO();
        wallet.setAvailableAmount(new BigDecimal(balance));
        wallet.setFrozenAmount(BigDecimal.ZERO);
        wallet.setTotalRecharge(BigDecimal.ZERO);
        wallet.setTotalConsume(BigDecimal.ZERO);
        return wallet;
    }

    private AppCreateOrderItemDTO buildItem(Long productId, Integer quantity) {
        AppCreateOrderItemDTO item = new AppCreateOrderItemDTO();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private AppCreateOrderItemDTO buildItemWithPrice(Long productId, Integer quantity, String price) {
        AppCreateOrderItemDTO item = buildItem(productId, quantity);
        item.setPrice(new BigDecimal(price));
        return item;
    }

    private ProductStock buildStock(Long productId, Integer quantity) {
        ProductStock stock = new ProductStock();
        stock.setProductId(productId);
        stock.setQuantity(quantity);
        return stock;
    }

    private OrderPricingResultVO buildPricingResult(String totalAmount, String payableAmount) {
        OrderPricingResultVO result = new OrderPricingResultVO();
        result.setTotalAmount(new BigDecimal(totalAmount));
        result.setPayableAmount(new BigDecimal(payableAmount));
        result.setActivityDiscountAmount(BigDecimal.ZERO);
        result.setCouponDiscountAmount(BigDecimal.ZERO);
        result.setPointsDeductAmount(BigDecimal.ZERO);
        return result;
    }
}
