package com.payment.vo;

import com.payment.entity.RefundApplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;
import com.payment.util.JsonUtils;

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
    private List<String> evidenceUrls;
    private String rejectReason;
    private String deliveryStatus;
    private Long refundableAmount;
    private Boolean quickRefundSuggested;
    private String refundSuggestion;
    private String statusLabel;
    private String statusDescription;
    private String nextStep;
    private String failureReason;
    private List<String> availableActions;
    private String auditTime;
    private String completeTime;
    private String createTime;
    private String updateTime;

    public static RefundApplicationVO from(RefundApplication app) {
        if (app == null) {
            return null;
        }
        StatusPresentation presentation = RefundStatusPresentation.from(app);
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
                .evidenceUrls(app.getEvidenceUrlsJson() == null || app.getEvidenceUrlsJson().isBlank()
                        ? List.of() : JsonUtils.fromJson(app.getEvidenceUrlsJson(), new TypeReference<List<String>>() { }))
                .rejectReason(app.getRejectReason())
                .deliveryStatus(app.getDeliveryStatus())
                .refundableAmount(VoConverterUtil.toFen(app.getRefundableAmount()))
                .quickRefundSuggested(app.getQuickRefundSuggested())
                .refundSuggestion(app.getRefundSuggestion())
                .statusLabel(presentation.statusLabel())
                .statusDescription(presentation.statusDescription())
                .nextStep(presentation.nextStep())
                .failureReason(presentation.failureReason())
                .availableActions(presentation.availableActions())
                .auditTime(VoConverterUtil.formatTime(app.getAuditTime()))
                .completeTime(VoConverterUtil.formatTime(app.getCompleteTime()))
                .createTime(VoConverterUtil.formatTime(app.getCreateTime()))
                .updateTime(VoConverterUtil.formatTime(app.getUpdateTime()))
                .build();
    }
}
