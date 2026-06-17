package com.payment.vo;

import com.payment.entity.RefundApplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String refundType;
    private String refundStatus;
    private Long refundAmount;
    private String reason;
    private String description;
    private String rejectReason;
    private String deliveryStatus;
    private Long refundableAmount;
    private Boolean quickRefundSuggested;
    private String refundSuggestion;
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
                .refundType(app.getRefundType())
                .refundStatus(app.getRefundStatus())
                .refundAmount(VoConverterUtil.toFen(app.getRefundAmount()))
                .reason(app.getReason())
                .description(app.getDescription())
                .rejectReason(app.getRejectReason())
                .deliveryStatus(app.getDeliveryStatus())
                .refundableAmount(VoConverterUtil.toFen(app.getRefundableAmount()))
                .quickRefundSuggested(app.getQuickRefundSuggested())
                .refundSuggestion(app.getRefundSuggestion())
                .auditTime(VoConverterUtil.formatTime(app.getAuditTime()))
                .completeTime(VoConverterUtil.formatTime(app.getCompleteTime()))
                .createTime(VoConverterUtil.formatTime(app.getCreateTime()))
                .updateTime(VoConverterUtil.formatTime(app.getUpdateTime()))
                .build();
    }
}
