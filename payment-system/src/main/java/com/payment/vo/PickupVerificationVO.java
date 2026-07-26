package com.payment.vo;

import com.payment.entity.OrderDeliveryRecord;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PickupVerificationVO {
    private Long orderId;
    private String orderNo;
    private Long orderItemId;
    private Long storeId;
    private String status;
    private LocalDateTime verifiedTime;

    public static PickupVerificationVO from(OrderDeliveryRecord record, Long storeId) {
        PickupVerificationVO vo = new PickupVerificationVO();
        vo.setOrderId(record.getOrderId());
        vo.setOrderNo(record.getOrderNo());
        vo.setOrderItemId(record.getOrderItemId());
        vo.setStoreId(storeId);
        vo.setStatus(record.getStatus());
        vo.setVerifiedTime(record.getConfirmedTime());
        return vo;
    }
}
