package com.payment.vo;

import com.payment.entity.OrderDeliveryRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderDeliveryVoTest {

    @Test
    void fromShouldExposeProductNameSnapshotForPurchaseList() {
        OrderDeliveryRecord record = new OrderDeliveryRecord();
        record.setId(11L);
        record.setTenantId(9L);
        record.setOrderNo("SO202607050001");
        record.setProductId(22L);
        record.setProductName("课程资料包");
        record.setProductType("VIRTUAL");
        record.setStatus("DELIVERED");

        OrderDeliveryVO vo = OrderDeliveryVO.from(record);

        assertThat(vo.getTenantId()).isEqualTo(9L);
        assertThat(vo.getProductName()).isEqualTo("课程资料包");
    }
}
