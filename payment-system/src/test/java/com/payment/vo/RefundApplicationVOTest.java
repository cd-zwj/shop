package com.payment.vo;

import com.payment.entity.RefundApplication;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RefundApplicationVOTest {

    @Test
    void pendingRefundShouldExposeReviewNextStep() {
        RefundApplication app = refund("PENDING");
        app.setRefundSuggestion("未发货/未交付，商家同意后可快速进入渠道退款");

        RefundApplicationVO vo = RefundApplicationVO.from(app);

        assertThat(vo.getStatusLabel()).isEqualTo("待商家审核");
        assertThat(vo.getStatusDescription()).contains("商家");
        assertThat(vo.getNextStep()).contains("审核");
        assertThat(vo.getAvailableActions()).contains("CANCEL_REFUND", "CONTACT_MERCHANT");
    }

    @Test
    void processingRefundShouldExposeExpectedProcessingNode() {
        RefundApplication voSource = refund("PROCESSING");

        RefundApplicationVO vo = RefundApplicationVO.from(voSource);

        assertThat(vo.getStatusLabel()).isEqualTo("退款处理中");
        assertThat(vo.getNextStep()).contains("内部退款单完成");
        assertThat(vo.getAvailableActions()).contains("CONTACT_MERCHANT");
    }

    @Test
    void failedRefundShouldExposeFailureReasonAndRetryAction() {
        RefundApplication app = refund("FAILED");
        app.setRejectReason("交付撤销失败，请人工处理后再退款");

        RefundApplicationVO vo = RefundApplicationVO.from(app);

        assertThat(vo.getStatusLabel()).isEqualTo("退款失败");
        assertThat(vo.getFailureReason()).isEqualTo("交付撤销失败，请人工处理后再退款");
        assertThat(vo.getAvailableActions()).contains("CONTACT_MERCHANT", "APPLY_REFUND");
    }

    private RefundApplication refund(String status) {
        RefundApplication app = new RefundApplication();
        app.setId(1L);
        app.setRefundNo("RA202607070001");
        app.setOrderNo("SO202607070001");
        app.setRefundStatus(status);
        app.setRefundType("REFUND_ONLY");
        app.setRefundAmount(new BigDecimal("10.00"));
        app.setReason("不想要了");
        return app;
    }
}
