package com.payment.vo;

import com.payment.entity.RefundApplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 退款申请视图对象，过滤内部字段，金额转为分（Long）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundApplicationVO {

    private Long id;
    private String refundNo;
    private String orderNo;
    private Long orderItemId;
    private Long tenantId;
    private String refundType;
    private String refundStatus;
    private Long refundAmount;
    private String reason;
    private String description;
    private String rejectReason;
    private String auditTime;
    private String completeTime;
    private String createTime;
    private String updateTime;

    public static RefundApplicationVO from(RefundApplication app) {
        if (app == null) {
            return null;
        }
        return RefundApplicationVO.builder()
                .id(app.getId())
                .refundNo(app.getRefundNo())
                .orderNo(app.getOrderNo())
                .orderItemId(app.getOrderItemId())
                .tenantId(app.getTenantId())
                .refundType(app.getRefundType())
                .refundStatus(app.getRefundStatus())
                .refundAmount(toFen(app.getRefundAmount()))
                .reason(app.getReason())
                .description(app.getDescription())
                .rejectReason(app.getRejectReason())
                .auditTime(formatTime(app.getAuditTime()))
                .completeTime(formatTime(app.getCompleteTime()))
                .createTime(formatTime(app.getCreateTime()))
                .updateTime(formatTime(app.getUpdateTime()))
                .build();
    }

    private static Long toFen(BigDecimal amount) {
        return amount == null ? null : amount.multiply(new BigDecimal(100)).longValue();
    }

    private static String formatTime(java.time.LocalDateTime time) {
        return time == null ? null : time.toString();
    }
}
