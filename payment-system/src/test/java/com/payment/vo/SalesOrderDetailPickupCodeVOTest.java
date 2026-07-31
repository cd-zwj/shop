package com.payment.vo;

import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SalesOrderDetailPickupCodeVOTest {

    @Test
    void fromShouldExposePickupCodeOnlyOnMatchingOrderItem() {
        SalesOrder order = new SalesOrder();
        order.setId(1L);
        order.setOrderNo("SO001");
        SalesOrderItem item = new SalesOrderItem();
        item.setId(11L);
        item.setProductName("测试商品");
        com.payment.dto.SalesOrderDetailVO detail = new com.payment.dto.SalesOrderDetailVO();
        detail.setOrder(order);
        detail.setItems(List.of(item));
        detail.setPickupCodesByOrderItemId(Map.of(11L, "12345678"));

        SalesOrderDetailVO result = SalesOrderDetailVO.from(detail);

        assertThat(result.getItems()).singleElement()
                .extracting(SalesOrderDetailVO.SalesOrderItemVO::getPickupCode)
                .isEqualTo("12345678");
    }
}
