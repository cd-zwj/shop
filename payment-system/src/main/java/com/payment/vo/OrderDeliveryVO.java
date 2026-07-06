package com.payment.vo;

import com.payment.entity.OrderDeliveryRecord;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 交付记录 VO，用于 C 端"我的已购"页面。
 */
@Data
public class OrderDeliveryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long orderId;
    private String orderNo;
    private Long orderItemId;
    private Long productId;
    private String productName;
    private String productType;
    private String status;
    /** 交付内容(JSON 字符串),前端按 productType 解读 */
    private String payload;
    private String failReason;
    private LocalDateTime deliveredTime;
    private LocalDateTime confirmedTime;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;

    public static OrderDeliveryVO from(OrderDeliveryRecord entity) {
        if (entity == null) {
            return null;
        }
        OrderDeliveryVO vo = new OrderDeliveryVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setOrderId(entity.getOrderId());
        vo.setOrderNo(entity.getOrderNo());
        vo.setOrderItemId(entity.getOrderItemId());
        vo.setProductId(entity.getProductId());
        vo.setProductName(entity.getProductName());
        vo.setProductType(entity.getProductType());
        vo.setStatus(entity.getStatus());
        vo.setPayload(entity.getPayload());
        vo.setFailReason(entity.getFailReason());
        vo.setDeliveredTime(entity.getDeliveredTime());
        vo.setConfirmedTime(entity.getConfirmedTime());
        vo.setExpireTime(entity.getExpireTime());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
