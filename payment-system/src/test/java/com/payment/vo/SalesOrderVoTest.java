package com.payment.vo;

import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SalesOrderVoTest {

    @Test
    void listVoKeepsIdentityFieldsNeededByAppAndMerchantPages() {
        SalesOrder order = buildOrder();

        SalesOrderListVO vo = SalesOrderListVO.from(order);

        assertThat(vo.getId()).isEqualTo(11L);
        assertThat(vo.getTenantId()).isEqualTo(22L);
        assertThat(vo.getPlatformUserId()).isEqualTo(33L);
        assertThat(vo.getOrderNo()).isEqualTo("SO202607050001");
    }

    @Test
    void listVoExposesUserVisibleStatusAndActionsForPendingPayment() {
        SalesOrder order = buildOrder();
        order.setOrderStatus("CREATED");
        order.setPayStatus("WAIT_PAY");

        SalesOrderListVO vo = SalesOrderListVO.from(order);

        assertThat(vo.getStatusLabel()).isEqualTo("待支付");
        assertThat(vo.getStatusDescription()).contains("订单已创建");
        assertThat(vo.getNextStep()).contains("继续支付");
        assertThat(vo.getAvailableActions()).contains("PAY", "CANCEL");
    }

    @Test
    void listVoExposesFailureReasonForFailedPayment() {
        SalesOrder order = buildOrder();
        order.setOrderStatus("CREATED");
        order.setPayStatus("FAILED");

        SalesOrderListVO vo = SalesOrderListVO.from(order);

        assertThat(vo.getStatusLabel()).isEqualTo("支付失败");
        assertThat(vo.getFailureReason()).contains("支付渠道");
        assertThat(vo.getAvailableActions()).contains("PAY", "CONTACT_MERCHANT", "REPURCHASE");
    }

    @Test
    void detailVoKeepsIdentityFieldsNeededByRefundAndWorkbenchFlows() {
        SalesOrder order = buildOrder();
        order.setShippingAddressId(55L);
        order.setShippingReceiverName("张三");
        order.setShippingPhone("13800000000");
        order.setShippingProvince("浙江省");
        order.setShippingCity("杭州市");
        order.setShippingDistrict("西湖区");
        order.setShippingDetail("文三路 1 号");
        com.payment.dto.SalesOrderDetailVO detail = new com.payment.dto.SalesOrderDetailVO();
        detail.setOrder(order);
        detail.setPaymentBillNo("PB202607050001");
        detail.setPaymentBillStatus("FAILED");
        detail.setPaymentBillStatusRemark("渠道返回：用户取消支付");
        detail.setPaymentBillExpireTime(LocalDateTime.of(2026, 7, 5, 10, 30));

        SalesOrderDetailVO vo = SalesOrderDetailVO.from(detail);

        assertThat(vo.getId()).isEqualTo(11L);
        assertThat(vo.getTenantId()).isEqualTo(22L);
        assertThat(vo.getPlatformUserId()).isEqualTo(33L);
        assertThat(vo.getPaymentBillNo()).isEqualTo("PB202607050001");
        assertThat(vo.getPaymentBillStatus()).isEqualTo("FAILED");
        assertThat(vo.getPaymentBillStatusRemark()).isEqualTo("渠道返回：用户取消支付");
        assertThat(vo.getPaymentBillExpireTime()).isEqualTo("2026-07-05T10:30");
        assertThat(vo.getShippingAddressId()).isEqualTo(55L);
        assertThat(vo.getShippingReceiverName()).isEqualTo("张三");
        assertThat(vo.getShippingPhone()).isEqualTo("13800000000");
        assertThat(vo.getShippingCity()).isEqualTo("杭州市");
        assertThat(vo.getShippingDetail()).isEqualTo("文三路 1 号");
    }

    @Test
    void detailVoUsesPaymentBillRemarkAsFailureReason() {
        SalesOrder order = buildOrder();
        order.setOrderStatus("CREATED");
        order.setPayStatus("WAIT_PAY");
        com.payment.dto.SalesOrderDetailVO detail = new com.payment.dto.SalesOrderDetailVO();
        detail.setOrder(order);
        detail.setPaymentBillStatus("FAILED");
        detail.setPaymentBillStatusRemark("渠道返回：用户取消支付");

        SalesOrderDetailVO vo = SalesOrderDetailVO.from(detail);

        assertThat(vo.getStatusLabel()).isEqualTo("支付失败");
        assertThat(vo.getFailureReason()).isEqualTo("渠道返回：用户取消支付");
        assertThat(vo.getNextStep()).contains("重新发起支付");
    }

    @Test
    void detailVoExplainsExpiredOrClosedPaymentBillBeforeGenericPendingPayment() {
        SalesOrder order = buildOrder();
        order.setOrderStatus("CREATED");
        order.setPayStatus("WAIT_PAY");
        com.payment.dto.SalesOrderDetailVO expiredDetail = new com.payment.dto.SalesOrderDetailVO();
        expiredDetail.setOrder(order);
        expiredDetail.setPaymentBillStatus("EXPIRED");
        expiredDetail.setPaymentBillStatusRemark("支付单已超过 30 分钟有效期");

        SalesOrderDetailVO expiredVo = SalesOrderDetailVO.from(expiredDetail);

        assertThat(expiredVo.getStatusLabel()).isEqualTo("支付已过期");
        assertThat(expiredVo.getFailureReason()).isEqualTo("支付单已超过 30 分钟有效期");
        assertThat(expiredVo.getNextStep()).contains("重新发起支付");
        assertThat(expiredVo.getAvailableActions()).contains("PAY", "CONTACT_MERCHANT");

        com.payment.dto.SalesOrderDetailVO closedDetail = new com.payment.dto.SalesOrderDetailVO();
        closedDetail.setOrder(order);
        closedDetail.setPaymentBillStatus("CLOSED");
        closedDetail.setPaymentBillStatusRemark("用户主动关闭支付页");

        SalesOrderDetailVO closedVo = SalesOrderDetailVO.from(closedDetail);

        assertThat(closedVo.getStatusLabel()).isEqualTo("支付已关闭");
        assertThat(closedVo.getFailureReason()).isEqualTo("用户主动关闭支付页");
        assertThat(closedVo.getNextStep()).contains("重新发起支付");
        assertThat(closedVo.getAvailableActions()).contains("PAY", "CONTACT_MERCHANT");
    }

    @Test
    void detailVoUsesDeliveryItemsToExplainFulfillmentState() {
        SalesOrder order = buildOrder();
        order.setOrderStatus("PAID");
        order.setPayStatus("SUCCESS");
        SalesOrderItem item = new SalesOrderItem();
        item.setId(101L);
        item.setProductId(9L);
        item.setProductName("虚拟卡密");
        item.setPrice(new BigDecimal("10.00"));
        item.setQuantity(1);
        item.setSubtotal(new BigDecimal("10.00"));
        item.setProductType("CARD_KEY");
        item.setDeliveryStatus("DELIVERED");
        com.payment.dto.SalesOrderDetailVO detail = new com.payment.dto.SalesOrderDetailVO();
        detail.setOrder(order);
        detail.setItems(List.of(item));

        SalesOrderDetailVO vo = SalesOrderDetailVO.from(detail);

        assertThat(vo.getStatusLabel()).isEqualTo("已发货");
        assertThat(vo.getStatusDescription()).contains("已交付");
        assertThat(vo.getNextStep()).contains("查看卡密");
        assertThat(vo.getAvailableActions()).contains("VIEW_DELIVERY", "REFUND");
    }

    private SalesOrder buildOrder() {
        SalesOrder order = new SalesOrder();
        order.setId(11L);
        order.setTenantId(22L);
        order.setPlatformUserId(33L);
        order.setOrderNo("SO202607050001");
        order.setOrderStatus("PAID");
        order.setPayStatus("SUCCESS");
        order.setTotalAmount(new BigDecimal("88.00"));
        order.setPayableAmount(new BigDecimal("80.00"));
        order.setCreateTime(LocalDateTime.of(2026, 7, 5, 10, 0));
        return order;
    }
}
