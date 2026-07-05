package com.payment.vo;

import com.payment.entity.SalesOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
