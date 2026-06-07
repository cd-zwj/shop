package com.payment.vo;

import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端订单详情视图对象，替代 SalesOrderDetailVO 中直接嵌套 Entity 的问题。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderDetailVO {

    private String orderNo;
    private Long tenantId;
    private String orderStatus;
    private String payStatus;
    private Long totalAmount;
    private Long discountAmount;
    private Long walletDeductAmount;
    private Long pointsDeductAmount;
    private Long unifiedWalletDeductAmount;
    private Long merchantWalletDeductAmount;
    private Long externalPayAmount;
    private Long payableAmount;
    private String subject;
    private String source;
    private String walletStrategy;
    private String expireTime;
    private String createTime;
    private String updateTime;

    private List<SalesOrderItemVO> items;
    private String paymentBillNo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesOrderItemVO {
        private Long id;
        private String productName;
        private Long price;
        private Integer quantity;
        private Long subtotal;
    }

    public static SalesOrderDetailVO from(com.payment.dto.SalesOrderDetailVO detailVO) {
        if (detailVO == null || detailVO.getOrder() == null) {
            return null;
        }
        SalesOrder order = detailVO.getOrder();
        SalesOrderDetailVO vo = SalesOrderDetailVO.builder()
                .orderNo(order.getOrderNo())
                .tenantId(order.getTenantId())
                .orderStatus(order.getOrderStatus())
                .payStatus(order.getPayStatus())
                .totalAmount(toFen(order.getTotalAmount()))
                .discountAmount(toFen(order.getDiscountAmount()))
                .walletDeductAmount(toFen(order.getWalletDeductAmount()))
                .pointsDeductAmount(toFen(order.getPointsDeductAmount()))
                .unifiedWalletDeductAmount(toFen(order.getUnifiedWalletDeductAmount()))
                .merchantWalletDeductAmount(toFen(order.getMerchantWalletDeductAmount()))
                .externalPayAmount(toFen(order.getExternalPayAmount()))
                .payableAmount(toFen(order.getPayableAmount()))
                .subject(order.getSubject())
                .source(order.getSource())
                .walletStrategy(order.getWalletStrategy())
                .expireTime(formatTime(order.getExpireTime()))
                .createTime(formatTime(order.getCreateTime()))
                .updateTime(formatTime(order.getUpdateTime()))
                .paymentBillNo(detailVO.getPaymentBillNo())
                .build();

        if (detailVO.getItems() != null) {
            vo.setItems(detailVO.getItems().stream()
                    .map(SalesOrderDetailVO::toItemVO)
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    private static SalesOrderItemVO toItemVO(SalesOrderItem item) {
        return SalesOrderItemVO.builder()
                .id(item.getId())
                .productName(item.getProductName())
                .price(toFen(item.getPrice()))
                .quantity(item.getQuantity())
                .subtotal(toFen(item.getSubtotal()))
                .build();
    }

    private static Long toFen(BigDecimal amount) {
        return amount == null ? null : amount.multiply(new BigDecimal(100)).longValue();
    }

    private static String formatTime(java.time.LocalDateTime time) {
        return time == null ? null : time.toString();
    }
}
