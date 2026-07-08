package com.payment.vo;

import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户端订单列表视图对象，隐藏内部字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderListVO {

    private Long id;
    private String orderNo;
    private Long tenantId;
    private Long platformUserId;
    private String orderStatus;
    private String payStatus;
    private Long totalAmount;
    private Long discountAmount;
    private Long walletDeductAmount;
    private Long pointsDeductAmount;
    private Long externalPayAmount;
    private Long payableAmount;
    private String subject;
    private String source;
    private String expireTime;
    private String createTime;
    private String updateTime;
    private String statusLabel;
    private String statusDescription;
    private String nextStep;
    private String failureReason;
    private List<String> availableActions;
    private String deliveryStatus;
    private OrderDeliverySummaryVO deliverySummary;

    public static SalesOrderListVO from(SalesOrder order) {
        return from(order, List.of());
    }

    public static SalesOrderListVO from(SalesOrder order, List<SalesOrderItem> items) {
        if (order == null) {
            return null;
        }
        List<SalesOrderItem> safeItems = items == null ? List.of() : items;
        StatusPresentation presentation = OrderStatusPresentation.from(order, safeItems, null, null);
        OrderDeliverySummaryVO deliverySummary = OrderDeliverySummaryVO.from(safeItems);
        return SalesOrderListVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .tenantId(order.getTenantId())
                .platformUserId(order.getPlatformUserId())
                .orderStatus(order.getOrderStatus())
                .payStatus(order.getPayStatus())
                .totalAmount(VoConverterUtil.toFen(order.getTotalAmount()))
                .discountAmount(VoConverterUtil.toFen(order.getDiscountAmount()))
                .walletDeductAmount(VoConverterUtil.toFen(order.getWalletDeductAmount()))
                .pointsDeductAmount(VoConverterUtil.toFen(order.getPointsDeductAmount()))
                .externalPayAmount(VoConverterUtil.toFen(order.getExternalPayAmount()))
                .payableAmount(VoConverterUtil.toFen(order.getPayableAmount()))
                .subject(order.getSubject())
                .source(order.getSource())
                .expireTime(VoConverterUtil.formatTime(order.getExpireTime()))
                .createTime(VoConverterUtil.formatTime(order.getCreateTime()))
                .updateTime(VoConverterUtil.formatTime(order.getUpdateTime()))
                .statusLabel(presentation.statusLabel())
                .statusDescription(presentation.statusDescription())
                .nextStep(presentation.nextStep())
                .failureReason(presentation.failureReason())
                .availableActions(presentation.availableActions())
                .deliveryStatus(deliverySummary.getPrimaryStatus())
                .deliverySummary(deliverySummary)
                .build();
    }
}
