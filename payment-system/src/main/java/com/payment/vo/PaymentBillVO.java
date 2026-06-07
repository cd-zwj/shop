package com.payment.vo;

import com.payment.entity.PaymentBill;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户端支付单视图对象，隐藏 platformUserId 等内部字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentBillVO {

    private String billNo;
    private String bizType;
    private String bizNo;
    private String channelCode;
    private String channelMode;
    private Long payAmount;
    private String payStatus;
    private String thirdPartyBillNo;
    private String callbackStatus;
    private String statusRemark;
    private String expireTime;
    private String createTime;
    private String updateTime;

    public static PaymentBillVO from(PaymentBill bill) {
        if (bill == null) {
            return null;
        }
        return PaymentBillVO.builder()
                .billNo(bill.getBillNo())
                .bizType(bill.getBizType())
                .bizNo(bill.getBizNo())
                .channelCode(bill.getChannelCode())
                .channelMode(bill.getChannelMode())
                .payAmount(VoConverterUtil.toFen(bill.getPayAmount()))
                .payStatus(bill.getPayStatus())
                .thirdPartyBillNo(bill.getThirdPartyBillNo())
                .callbackStatus(bill.getCallbackStatus())
                .statusRemark(bill.getStatusRemark())
                .expireTime(VoConverterUtil.formatTime(bill.getExpireTime()))
                .createTime(VoConverterUtil.formatTime(bill.getCreateTime()))
                .updateTime(VoConverterUtil.formatTime(bill.getUpdateTime()))
                .build();
    }
}
