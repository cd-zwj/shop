package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.dto.AppCreateOrderDTO;
import com.payment.dto.AppCreateOrderItemDTO;
import com.payment.dto.OrderPaymentVO;
import com.payment.dto.PayResponseDTO;
import com.payment.dto.WalletAccountVO;
import com.payment.entity.PaymentBill;
import com.payment.entity.Product;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.entity.TenantEmployee;
import com.payment.entity.TenantMember;
import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.mapper.PointsRuleMapper;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.mapper.TenantEmployeeMapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MerchantWalletService;
import com.payment.service.PaymentBillV1Service;
import com.payment.service.UnifiedWalletService;
import com.payment.service.WithdrawalService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
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
                pointsRuleMapper
        );

        when(tenantMemberMapper.selectOne(any())).thenReturn(new TenantMember());
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(
                buildProduct(1L, 9L, "可乐", "3.50"),
                buildProduct(2L, 9L, "雪碧", "4.00")
        ));
        when(unifiedWalletService.getWallet(100L)).thenReturn(buildWallet("0.00"));
        when(merchantWalletService.getWallet(9L, 100L)).thenReturn(buildWallet("0.00"));

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

        ArgumentCaptor<SalesOrderItem> itemCaptor = ArgumentCaptor.forClass(SalesOrderItem.class);
        verify(salesOrderItemMapper, times(2)).insert(itemCaptor.capture());
        List<SalesOrderItem> items = itemCaptor.getAllValues();
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
                pointsRuleMapper
        );

        when(tenantMemberMapper.selectOne(any())).thenReturn(new TenantMember());
        when(productMapper.selectBatchIds(any())).thenReturn(List.of(buildProduct(3L, 9L, "奶茶", "12.00")));
        when(unifiedWalletService.getWallet(100L)).thenReturn(buildWallet("20.00"));
        when(merchantWalletService.getWallet(9L, 100L)).thenReturn(buildWallet("0.00"));

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
        verify(salesOrderItemMapper, times(1)).insert(any(SalesOrderItem.class));
        assertNull(result.getPaymentBillNo());
        assertEquals("PAID", result.getOrderStatus());
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
                pointsRuleMapper
        );

        when(productMapper.selectBatchIds(any())).thenReturn(List.of(buildProduct(1L, 10L, "别家商品", "9.90")));

        AppCreateOrderDTO dto = new AppCreateOrderDTO();
        dto.setTenantId(9L);
        dto.setWalletStrategy(com.payment.enums.WalletStrategyEnum.NO_WALLET);
        dto.setItems(List.of(buildItem(1L, 1)));

        assertThrows(BusinessException.class, () -> service.createOrder(100L, dto));
        verify(salesOrderMapper, never()).insert(any(SalesOrder.class));
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
                pointsRuleMapper
        );

        when(tenantEmployeeMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.getMerchantOrderDetail(9L, 100L, "SO001"));
    }

    private Product buildProduct(Long id, Long tenantId, String name, String price) {
        Product product = new Product();
        product.setId(id);
        product.setTenantId(tenantId);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
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
}
