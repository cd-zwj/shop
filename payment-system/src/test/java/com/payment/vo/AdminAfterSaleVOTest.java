package com.payment.vo;

import com.payment.entity.RefundApplication;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AdminAfterSaleVOTest {

    @Test
    void shouldExposeTenantAndOperationalFieldsWithoutPlatformUserId() {
        RefundApplication application = new RefundApplication();
        application.setId(8L);
        application.setTenantId(9L);
        application.setPlatformUserId(10L);
        application.setRefundNo("RA-8");
        application.setOrderNo("SO-8");
        application.setRefundType("REFUND_ONLY");
        application.setRefundStatus("PENDING");
        application.setRefundAmount(new BigDecimal("12.34"));
        application.setReason("商品问题");

        AdminAfterSaleVO result = AdminAfterSaleVO.from(application);

        assertEquals(9L, result.getTenantId());
        assertEquals(1234L, result.getRefundAmount());
        assertEquals("PENDING", result.getRefundStatus());
        assertFalse(java.util.Arrays.stream(AdminAfterSaleVO.class.getDeclaredFields())
                .anyMatch(field -> "platformUserId".equals(field.getName())));
    }
}
