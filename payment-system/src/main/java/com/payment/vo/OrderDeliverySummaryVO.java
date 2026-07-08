package com.payment.vo;

import com.payment.entity.SalesOrderItem;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 订单列表页使用的交付状态汇总。
 */
@Data
@Builder
public class OrderDeliverySummaryVO {

    private int totalCount;
    private int pendingCount;
    private int deliveringCount;
    private int deliveredCount;
    private int confirmedCount;
    private int failedCount;
    private int revokedCount;
    private int revokeFailedCount;
    private String primaryStatus;

    public static OrderDeliverySummaryVO from(List<SalesOrderItem> items) {
        List<SalesOrderItem> safeItems = items == null ? List.of() : items;
        int pendingCount = count(safeItems, "PENDING", true);
        int deliveringCount = count(safeItems, "DELIVERING", false);
        int deliveredCount = count(safeItems, "DELIVERED", false);
        int confirmedCount = count(safeItems, "CONFIRMED", false);
        int failedCount = count(safeItems, "FAILED", false);
        int revokedCount = count(safeItems, "REVOKED", false);
        int revokeFailedCount = count(safeItems, "REVOKE_FAILED", false);

        return OrderDeliverySummaryVO.builder()
                .totalCount(safeItems.size())
                .pendingCount(pendingCount)
                .deliveringCount(deliveringCount)
                .deliveredCount(deliveredCount)
                .confirmedCount(confirmedCount)
                .failedCount(failedCount)
                .revokedCount(revokedCount)
                .revokeFailedCount(revokeFailedCount)
                .primaryStatus(resolvePrimaryStatus(
                        pendingCount,
                        deliveringCount,
                        deliveredCount,
                        confirmedCount,
                        failedCount,
                        revokedCount,
                        revokeFailedCount))
                .build();
    }

    private static int count(List<SalesOrderItem> items, String status, boolean includeBlankAsPending) {
        return (int) items.stream()
                .filter(item -> {
                    String itemStatus = item.getDeliveryStatus();
                    if (includeBlankAsPending && (itemStatus == null || itemStatus.isBlank())) {
                        return true;
                    }
                    return status.equals(itemStatus);
                })
                .count();
    }

    private static String resolvePrimaryStatus(int pendingCount,
                                               int deliveringCount,
                                               int deliveredCount,
                                               int confirmedCount,
                                               int failedCount,
                                               int revokedCount,
                                               int revokeFailedCount) {
        if (failedCount > 0) {
            return "FAILED";
        }
        if (revokeFailedCount > 0) {
            return "REVOKE_FAILED";
        }
        if (pendingCount > 0) {
            return "PENDING";
        }
        if (deliveringCount > 0) {
            return "DELIVERING";
        }
        if (deliveredCount > 0) {
            return "DELIVERED";
        }
        if (confirmedCount > 0) {
            return "CONFIRMED";
        }
        if (revokedCount > 0) {
            return "REVOKED";
        }
        return null;
    }
}
