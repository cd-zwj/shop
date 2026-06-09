package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户端领券结果视图对象。
 */
@Data
public class AppCouponReceiveVO {
    private Long userCouponId;
    private String couponNo;
    private Long templateId;
    private Long tenantId;
    private String couponStatus;
    private LocalDateTime expireTime;
}
