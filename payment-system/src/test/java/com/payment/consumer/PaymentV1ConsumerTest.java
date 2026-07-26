package com.payment.consumer;

import com.payment.config.RabbitMQConfig;
import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.PayStatusEnum;
import com.payment.mapper.PointsRuleMapper;
import com.payment.mapper.SalesOrderItemMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MemberService;
import com.payment.service.MerchantSettlementService;
import com.payment.service.MessageIdempotentService;
import com.payment.service.StoreInventoryService;
import com.payment.service.UserNotificationService;
import com.payment.service.WalletRechargeService;
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
    void handleRechargeSuccessShouldSkipProcessedMessage() {
        ConsumerFixture fixture = new ConsumerFixture();
        String body = "{\"bizNo\":\"RC001\"}";
        String messageId = RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE + ":RC001";
        when(fixture.messageIdempotentService.isProcessed(messageId, RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE))
                .thenReturn(true);

        fixture.consumer.handleRechargeSuccess(body);

        verify(fixture.walletRechargeService, never()).handleRechargeSuccess(any());
        verify(fixture.messageIdempotentService, never()).recordSuccess(any(), any(), any(), any());
    }

    @Test
    void handleRechargeSuccessShouldRecordSuccessAfterHandling() {
        ConsumerFixture fixture = new ConsumerFixture();
        String body = "{\"bizNo\":\"RC001\"}";
        String messageId = RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE + ":RC001";
        when(fixture.messageIdempotentService.isProcessed(messageId, RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE))
                .thenReturn(false);

        fixture.consumer.handleRechargeSuccess(body);

        verify(fixture.walletRechargeService).handleRechargeSuccess("RC001");
        verify(fixture.messageIdempotentService).recordSuccess(
                messageId,
                RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE,
                body,
                PaymentV1Consumer.class.getSimpleName());
    }

    @Test
    void handleRechargeSuccessShouldRecordFailureAndRethrow() {
        ConsumerFixture fixture = new ConsumerFixture();
        String body = "{\"bizNo\":\"RC001\"}";
        String messageId = RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE + ":RC001";
        when(fixture.messageIdempotentService.isProcessed(messageId, RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE))
                .thenReturn(false);
        org.mockito.Mockito.doThrow(new RuntimeException("recharge failed"))
                .when(fixture.walletRechargeService).handleRechargeSuccess("RC001");

        assertThrows(RuntimeException.class, () -> fixture.consumer.handleRechargeSuccess(body));

        verify(fixture.messageIdempotentService).recordFailure(
                messageId,
                RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE,
                body,
                PaymentV1Consumer.class.getSimpleName(),
                "recharge failed");
    }

    @Test
    void handleOrderPaidShouldNotMarkOrderPaidWhenStockDeductionFails() {
        ConsumerFixture fixture = new ConsumerFixture();

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setId(1L);
        salesOrder.setOrderNo("SO001");
        salesOrder.setTenantId(9L);
        salesOrder.setPlatformUserId(100L);
        salesOrder.setOrderStatus(OrderStatusEnum.PAID.name());
        salesOrder.setPayStatus(PayStatusEnum.SUCCESS.name());
        salesOrder.setTotalAmount(new BigDecimal("10.00"));
        salesOrder.setMerchantWalletDeductAmount(BigDecimal.ZERO);
        salesOrder.setFulfillmentMode("STORE_PICKUP");
        salesOrder.setStoreId(88L);
        salesOrder.setDeleted(0);

        SalesOrderItem item = new SalesOrderItem();
        item.setProductId(1L);
        item.setQuantity(2);

        when(fixture.salesOrderMapper.selectByOrderNoForUpdate(any())).thenReturn(salesOrder);
        when(fixture.salesOrderItemMapper.selectByOrderId(1L)).thenReturn(List.of(item));
        org.mockito.Mockito.doThrow(new RuntimeException("stock failed"))
                .when(fixture.storeInventoryService).deductLocked(
                        9L, 88L, 1L, 2, "SALES_ORDER", "SO001", 100L);

        assertThrows(RuntimeException.class, () -> fixture.consumer.handleOrderPaid("{\"bizNo\":\"SO001\"}"));
        verify(fixture.salesOrderMapper, never()).updateById(any(SalesOrder.class));
    }

    @Test
    void handleOrderPaidShouldDeductStoreStockForPickupOrder() {
        ConsumerFixture fixture = new ConsumerFixture();
        SalesOrder salesOrder = paidOrderFixture("SO_PICKUP");
        salesOrder.setFulfillmentMode("STORE_PICKUP");
        salesOrder.setStoreId(88L);
        SalesOrderItem item = orderItemFixture();

        when(fixture.messageIdempotentService.isProcessed(
                RabbitMQConfig.V1_ORDER_PAID_QUEUE + ":SO_PICKUP",
                RabbitMQConfig.V1_ORDER_PAID_QUEUE)).thenReturn(false);
        when(fixture.salesOrderMapper.selectByOrderNoForUpdate(any())).thenReturn(salesOrder);
        when(fixture.salesOrderItemMapper.selectByOrderId(1L)).thenReturn(List.of(item));

        fixture.consumer.handleOrderPaid("{\"bizNo\":\"SO_PICKUP\"}");

        verify(fixture.storeInventoryService).deductLocked(
                9L, 88L, 1L, 2, "SALES_ORDER", "SO_PICKUP", 100L);
    }

    @Test
    void handleOrderPaidShouldSettlePayableAmountMinusMerchantWalletDeduction() {
        ConsumerFixture fixture = new ConsumerFixture();
        SalesOrder salesOrder = paidOrderFixture("SO_PAYABLE");
        salesOrder.setTotalAmount(new BigDecimal("100.00"));
        salesOrder.setPayableAmount(new BigDecimal("80.00"));
        salesOrder.setMerchantWalletDeductAmount(new BigDecimal("20.00"));
        SalesOrderItem item = orderItemFixture();

        when(fixture.messageIdempotentService.isProcessed(
                RabbitMQConfig.V1_ORDER_PAID_QUEUE + ":SO_PAYABLE",
                RabbitMQConfig.V1_ORDER_PAID_QUEUE)).thenReturn(false);
        when(fixture.salesOrderMapper.selectByOrderNoForUpdate(any())).thenReturn(salesOrder);
        when(fixture.salesOrderItemMapper.selectByOrderId(1L)).thenReturn(List.of(item));

        fixture.consumer.handleOrderPaid("{\"bizNo\":\"SO_PAYABLE\"}");

        verify(fixture.settlementService).settleOrder(9L, new BigDecimal("60.00"), "SO_PAYABLE");
        verify(fixture.settlementService, never()).settleOrder(9L, new BigDecimal("80.00"), "SO_PAYABLE");
    }

    @Test
    void handleOrderPaidShouldSkipNonPositiveSettlementAmount() {
        ConsumerFixture fixture = new ConsumerFixture();
        SalesOrder salesOrder = paidOrderFixture("SO_ZERO");
        salesOrder.setTotalAmount(new BigDecimal("100.00"));
        salesOrder.setPayableAmount(new BigDecimal("20.00"));
        salesOrder.setMerchantWalletDeductAmount(new BigDecimal("20.00"));
        SalesOrderItem item = orderItemFixture();

        when(fixture.messageIdempotentService.isProcessed(
                RabbitMQConfig.V1_ORDER_PAID_QUEUE + ":SO_ZERO",
                RabbitMQConfig.V1_ORDER_PAID_QUEUE)).thenReturn(false);
        when(fixture.salesOrderMapper.selectByOrderNoForUpdate(any())).thenReturn(salesOrder);
        when(fixture.salesOrderItemMapper.selectByOrderId(1L)).thenReturn(List.of(item));

        fixture.consumer.handleOrderPaid("{\"bizNo\":\"SO_ZERO\"}");

        verify(fixture.settlementService, never()).settleOrder(any(), any(), any());
    }

    @Test
    void handleOrderPaidShouldDerivePayableAmountWhenStoredValueIsMissing() {
        ConsumerFixture fixture = new ConsumerFixture();
        SalesOrder salesOrder = paidOrderFixture("SO_LEGACY");
        salesOrder.setTotalAmount(new BigDecimal("100.00"));
        salesOrder.setDiscountAmount(new BigDecimal("10.00"));
        salesOrder.setUnifiedWalletDeductAmount(new BigDecimal("5.00"));
        salesOrder.setPointsDeductAmount(new BigDecimal("5.00"));
        salesOrder.setMerchantWalletDeductAmount(new BigDecimal("20.00"));
        SalesOrderItem item = orderItemFixture();

        when(fixture.messageIdempotentService.isProcessed(
                RabbitMQConfig.V1_ORDER_PAID_QUEUE + ":SO_LEGACY",
                RabbitMQConfig.V1_ORDER_PAID_QUEUE)).thenReturn(false);
        when(fixture.salesOrderMapper.selectByOrderNoForUpdate(any())).thenReturn(salesOrder);
        when(fixture.salesOrderItemMapper.selectByOrderId(1L)).thenReturn(List.of(item));

        fixture.consumer.handleOrderPaid("{\"bizNo\":\"SO_LEGACY\"}");

        verify(fixture.settlementService).settleOrder(9L, new BigDecimal("65.00"), "SO_LEGACY");
    }

    private static SalesOrder paidOrderFixture(String orderNo) {
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setId(1L);
        salesOrder.setOrderNo(orderNo);
        salesOrder.setTenantId(9L);
        salesOrder.setPlatformUserId(100L);
        salesOrder.setOrderStatus(OrderStatusEnum.PAID.name());
        salesOrder.setPayStatus(PayStatusEnum.SUCCESS.name());
        salesOrder.setFulfillmentMode("STORE_PICKUP");
        salesOrder.setStoreId(88L);
        salesOrder.setDeleted(0);
        return salesOrder;
    }

    private static SalesOrderItem orderItemFixture() {
        SalesOrderItem item = new SalesOrderItem();
        item.setProductId(1L);
        item.setQuantity(2);
        return item;
    }

    private static class ConsumerFixture {
        private final WalletRechargeService walletRechargeService = mock(WalletRechargeService.class);
        private final SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        private final SalesOrderItemMapper salesOrderItemMapper = mock(SalesOrderItemMapper.class);
        private final StoreInventoryService storeInventoryService = mock(StoreInventoryService.class);
        private final MerchantSettlementService settlementService = mock(MerchantSettlementService.class);
        private final MemberPointsAccountService memberPointsAccountService = mock(MemberPointsAccountService.class);
        private final MemberService memberService = mock(MemberService.class);
        private final PointsRuleMapper pointsRuleMapper = mock(PointsRuleMapper.class);
        private final UserNotificationService notificationService = mock(UserNotificationService.class);
        private final MessageIdempotentService messageIdempotentService = mock(MessageIdempotentService.class);
        private final com.payment.service.delivery.OrderDeliveryService orderDeliveryService = mock(com.payment.service.delivery.OrderDeliveryService.class);
        private final PaymentV1Consumer consumer;

        private ConsumerFixture() {
            when(salesOrderMapper.completePaymentProcessing(any())).thenReturn(1);
            consumer = new PaymentV1Consumer(
                    walletRechargeService,
                    salesOrderMapper,
                    salesOrderItemMapper,
                    storeInventoryService,
                    settlementService,
                    memberPointsAccountService,
                    pointsRuleMapper,
                    memberService,
                    notificationService,
                    messageIdempotentService,
                    orderDeliveryService
            );
        }
    }
}
