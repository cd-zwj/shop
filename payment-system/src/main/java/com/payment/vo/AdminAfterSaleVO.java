package com.payment.vo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.payment.entity.RefundApplication;
import com.payment.util.JsonUtils;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Platform after-sale view without customer identity fields. */
@Data
@Builder
public class AdminAfterSaleVO {
    private Long id;
    private Long tenantId;
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

    public static AdminAfterSaleVO from(RefundApplication application) {
        if (application == null) {
            return null;
        }
        StatusPresentation presentation = RefundStatusPresentation.from(application);
        return AdminAfterSaleVO.builder()
                .id(application.getId())
                .tenantId(application.getTenantId())
                .refundNo(application.getRefundNo())
                .orderNo(application.getOrderNo())
                .orderItemId(application.getOrderItemId())
                .refundType(application.getRefundType())
                .refundStatus(application.getRefundStatus())
                .refundAmount(VoConverterUtil.toFen(application.getRefundAmount()))
                .reason(application.getReason())
                .description(application.getDescription())
                .evidenceUrls(parseEvidence(application.getEvidenceUrlsJson()))
                .rejectReason(application.getRejectReason())
                .deliveryStatus(application.getDeliveryStatus())
                .refundableAmount(VoConverterUtil.toFen(application.getRefundableAmount()))
                .quickRefundSuggested(application.getQuickRefundSuggested())
                .refundSuggestion(application.getRefundSuggestion())
                .statusLabel(presentation.statusLabel())
                .statusDescription(presentation.statusDescription())
                .nextStep(presentation.nextStep())
                .failureReason(presentation.failureReason())
                .availableActions(presentation.availableActions())
                .auditTime(VoConverterUtil.formatTime(application.getAuditTime()))
                .completeTime(VoConverterUtil.formatTime(application.getCompleteTime()))
                .createTime(VoConverterUtil.formatTime(application.getCreateTime()))
                .updateTime(VoConverterUtil.formatTime(application.getUpdateTime()))
                .build();
    }

    private static List<String> parseEvidence(String evidenceJson) {
        return evidenceJson == null || evidenceJson.isBlank()
                ? List.of()
                : JsonUtils.fromJson(evidenceJson, new TypeReference<List<String>>() { });
    }
}
