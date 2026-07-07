package com.payment.vo;

import com.payment.entity.RefundApplication;

import java.util.List;

/**
 * 退款申请状态展示文案与可操作动作。
 */
public final class RefundStatusPresentation {

    private RefundStatusPresentation() {
    }

    public static StatusPresentation from(RefundApplication app) {
        if (app == null) {
            return new StatusPresentation("加载中", "正在同步退款申请状态。", "请稍候，系统正在读取最新售后信息。", null, List.of("DETAIL"));
        }

        String status = app.getRefundStatus();
        String suggestion = hasText(app.getRefundSuggestion()) ? app.getRefundSuggestion() : null;

        if ("PENDING".equals(status)) {
            return new StatusPresentation(
                    "待商家审核",
                    suggestion != null ? suggestion : "退款申请已提交，等待商家确认订单、交付和可退金额。",
                    "预计节点：商家审核后会进入退款处理或给出驳回原因。",
                    null,
                    List.of("CANCEL_REFUND", "CONTACT_MERCHANT"));
        }

        if ("APPROVED".equals(status) || "PROCESSING".equals(status)) {
            return new StatusPresentation(
                    "退款处理中",
                    suggestion != null ? suggestion : "商家已同意退款，系统正在处理内部退款单和交付回退。",
                    "预计节点：内部退款单完成后会更新为退款完成；失败时会显示失败原因。",
                    null,
                    List.of("CONTACT_MERCHANT"));
        }

        if ("COMPLETED".equals(status)) {
            return new StatusPresentation(
                    "退款完成",
                    "退款流程已完成，可在订单和资产明细中继续追溯。",
                    "后续无需操作，如金额未变化请联系商户核对本地账务记录。",
                    null,
                    List.of("DETAIL", "CONTACT_MERCHANT"));
        }

        if ("FAILED".equals(status)) {
            String reason = hasText(app.getRejectReason()) ? app.getRejectReason() : "内部退款处理失败，需要商家或平台重新处理。";
            return new StatusPresentation(
                    "退款失败",
                    "失败原因：" + reason,
                    "下一步：联系商户处理，或补充信息后重新提交售后申请。",
                    reason,
                    List.of("CONTACT_MERCHANT", "APPLY_REFUND"));
        }

        if ("REJECTED".equals(status)) {
            String reason = hasText(app.getRejectReason()) ? app.getRejectReason() : "商家已驳回本次退款申请。";
            return new StatusPresentation(
                    "已驳回",
                    "驳回原因：" + reason,
                    "下一步：如仍需售后，可补充原因后重新提交，或联系商户沟通。",
                    reason,
                    List.of("CONTACT_MERCHANT", "APPLY_REFUND"));
        }

        if ("CANCELLED".equals(status)) {
            return new StatusPresentation(
                    "已取消",
                    "该退款申请已取消，订单可按当前状态继续处理。",
                    "下一步：如仍需退款，可重新提交售后申请。",
                    null,
                    List.of("APPLY_REFUND"));
        }

        return new StatusPresentation(
                hasText(status) ? status : "未知状态",
                "该退款申请处于非常规状态，请查看订单或联系商户确认。",
                "下一步：联系商户核对售后处理进度。",
                null,
                List.of("CONTACT_MERCHANT"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
