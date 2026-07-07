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
    private Long unifiedWalletDeductAmount;
    private Long merchantWalletDeductAmount;
    private Long externalPayAmount;
    private Long payableAmount;
    private String subject;
    private String source;
    private Long shippingAddressId;
    private String shippingReceiverName;
    private String shippingPhone;
    private String shippingProvince;
    private String shippingCity;
    private String shippingDistrict;
    private String shippingDetail;
    private String walletStrategy;
    private String expireTime;
    private String createTime;
    private String updateTime;

    private List<SalesOrderItemVO> items;
    private String paymentBillNo;
    private String paymentBillStatus;
    private String paymentBillStatusRemark;
    private String paymentBillExpireTime;
    private String statusLabel;
    private String statusDescription;
    private String nextStep;
    private String failureReason;
    private List<String> availableActions;

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
        StatusPresentation presentation = OrderStatusPresentation.from(
                order,
                detailVO.getItems(),
                detailVO.getPaymentBillStatus(),
                detailVO.getPaymentBillStatusRemark());
        SalesOrderDetailVO vo = SalesOrderDetailVO.builder()
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
                .unifiedWalletDeductAmount(VoConverterUtil.toFen(order.getUnifiedWalletDeductAmount()))
                .merchantWalletDeductAmount(VoConverterUtil.toFen(order.getMerchantWalletDeductAmount()))
                .externalPayAmount(VoConverterUtil.toFen(order.getExternalPayAmount()))
                .payableAmount(VoConverterUtil.toFen(order.getPayableAmount()))
                .subject(order.getSubject())
                .source(order.getSource())
                .shippingAddressId(order.getShippingAddressId())
                .shippingReceiverName(order.getShippingReceiverName())
                .shippingPhone(order.getShippingPhone())
                .shippingProvince(order.getShippingProvince())
                .shippingCity(order.getShippingCity())
                .shippingDistrict(order.getShippingDistrict())
                .shippingDetail(order.getShippingDetail())
                .walletStrategy(order.getWalletStrategy())
                .expireTime(VoConverterUtil.formatTime(order.getExpireTime()))
                .createTime(VoConverterUtil.formatTime(order.getCreateTime()))
                .updateTime(VoConverterUtil.formatTime(order.getUpdateTime()))
                .paymentBillNo(detailVO.getPaymentBillNo())
                .paymentBillStatus(detailVO.getPaymentBillStatus())
                .paymentBillStatusRemark(detailVO.getPaymentBillStatusRemark())
                .paymentBillExpireTime(VoConverterUtil.formatTime(detailVO.getPaymentBillExpireTime()))
                .statusLabel(presentation.statusLabel())
                .statusDescription(presentation.statusDescription())
                .nextStep(presentation.nextStep())
                .failureReason(presentation.failureReason())
                .availableActions(presentation.availableActions())
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
