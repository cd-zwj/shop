package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户端领券结果视图对象，用于返回领取优惠券后的确认信息。
 */
@Data
public class AppCouponReceiveVO {
    /** 用户优惠券记录 ID */
    private Long userCouponId;
    /** 优惠券编号（唯一标识） */
    private String couponNo;
    /** 优惠券模板 ID */
    private Long templateId;
    /** 所属商户租户 ID */
    private Long tenantId;
    /** 优惠券状态（如 AVAILABLE-可用、USED-已使用、EXPIRED-已过期） */
    private String couponStatus;
    /** 过期时间 */
    private LocalDateTime expireTime;
}
