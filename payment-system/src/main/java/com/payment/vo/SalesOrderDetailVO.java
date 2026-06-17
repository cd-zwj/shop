package com.payment.vo;

import com.payment.entity.SalesOrder;
import com.payment.entity.SalesOrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
        private Long productId;
        private String productName;
        private Long price;
        private Integer quantity;
        private Long subtotal;
        private String productType;
        private String deliveryStatus;
        private String deliveredTime;
    }

    public static SalesOrderDetailVO from(com.payment.dto.SalesOrderDetailVO detailVO) {
        if (detailVO == null || detailVO.getOrder() == null) {
            return null;
        }
        SalesOrder order = detailVO.getOrder();
        SalesOrderDetailVO vo = SalesOrderDetailVO.builder()
                .orderNo(order.getOrderNo())
                .orderStatus(order.getOrderStatus())
                .payStatus(order.getPayStatus())
                .totalAmount(VoConverterUtil.toFen(order.getTotalAmount()))
                .discountAmount(VoConverterUtil.toFen(order.getDiscountAmount()))
                .walletDeductAmount(VoConverterUtil.toFen(order.getWalletDeductAmount()))
                .pointsDeductAmount(VoConverterUtil.toFen(order.getPointsDeductAmount()))
                .unifiedWalletDeductAmount(VoConverterUtil.toFen(order.getUnifiedWalletDeductAmount()))
                .merchantWalletDeductAmount(VoConverterUtil.toFen(order.getMerchantWalletDeductAmount()))
                .externalPayAmount(VoConverterUtil.toFen(order.getExternalPayAmount()))
                .payableAmount(VoConverterUtil.toFen(order.getPayableAmount()))
                .subject(order.getSubject())
                .source(order.getSource())
                .walletStrategy(order.getWalletStrategy())
                .expireTime(VoConverterUtil.formatTime(order.getExpireTime()))
                .createTime(VoConverterUtil.formatTime(order.getCreateTime()))
                .updateTime(VoConverterUtil.formatTime(order.getUpdateTime()))
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
                .productId(item.getProductId())
                .productName(item.getProductName())
                .price(VoConverterUtil.toFen(item.getPrice()))
                .quantity(item.getQuantity())
                .subtotal(VoConverterUtil.toFen(item.getSubtotal()))
                .productType(item.getProductType())
                .deliveryStatus(item.getDeliveryStatus())
                .deliveredTime(VoConverterUtil.formatTime(item.getDeliveredTime()))
                .build();
    }
}
