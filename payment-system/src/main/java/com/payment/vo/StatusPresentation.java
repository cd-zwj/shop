package com.payment.vo;

import java.util.List;

/**
 * 用户可见状态展示契约。
 */
public record StatusPresentation(
        String statusLabel,
        String statusDescription,
        String nextStep,
        String failureReason,
        List<String> availableActions
) {
}
