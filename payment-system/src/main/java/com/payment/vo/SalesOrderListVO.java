package com.payment.vo;

import com.payment.entity.SalesOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户端订单列表视图对象，隐藏内部字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderListVO {

    private String orderNo;
    private Long tenantId;
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

    public static SalesOrderListVO from(SalesOrder order) {
        if (order == null) {
            return null;
        }
        return SalesOrderListVO.builder()
                .orderNo(order.getOrderNo())
                .tenantId(order.getTenantId())
                .orderStatus(order.getOrderStatus())
                .payStatus(order.getPayStatus())
                .totalAmount(toFen(order.getTotalAmount()))
                .discountAmount(toFen(order.getDiscountAmount()))
                .walletDeductAmount(toFen(order.getWalletDeductAmount()))
                .pointsDeductAmount(toFen(order.getPointsDeductAmount()))
                .externalPayAmount(toFen(order.getExternalPayAmount()))
                .payableAmount(toFen(order.getPayableAmount()))
                .subject(order.getSubject())
                .source(order.getSource())
                .expireTime(formatTime(order.getExpireTime()))
                .createTime(formatTime(order.getCreateTime()))
                .updateTime(formatTime(order.getUpdateTime()))
                .build();
    }

    private static Long toFen(java.math.BigDecimal amount) {
        return amount == null ? null : amount.multiply(new java.math.BigDecimal(100)).longValue();
    }

    private static String formatTime(java.time.LocalDateTime time) {
        return time == null ? null : time.toString();
    }
}
