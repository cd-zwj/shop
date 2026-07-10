package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户端个人券包视图对象，用于展示用户已领取的优惠券详情及使用状态。
 */
@Data
public class AppUserCouponVO {
    /** 用户优惠券记录 ID */
    private Long id;
    /** 优惠券编号 */
    private String couponNo;
    /** 优惠券模板 ID */
    private Long templateId;
    /** 所属商户租户 ID */
    private Long tenantId;
    /** 优惠券状态（如 AVAILABLE-可用、USED-已使用、EXPIRED-已过期） */
    private String couponStatus;
    /** 使用或锁定该优惠券的订单号，用于用户侧追溯 */
    private String orderNo;
    /** 优惠券名称 */
    private String templateName;
    /** 优惠券类型（如 FIXED-满减、DISCOUNT-折扣） */
    private String couponType;
    /** 使用门槛金额 */
    private BigDecimal thresholdAmount;
    /** 固定减免金额 */
    private BigDecimal discountAmount;
    /** 折扣率 */
    private BigDecimal discountRate;
    /** 最大减免金额 */
    private BigDecimal maxDiscountAmount;
    /** 领取时间 */
    private LocalDateTime receiveTime;
    /** 过期时间 */
    private LocalDateTime expireTime;
    /** 使用时间 */
    private LocalDateTime useTime;
    /** 用户端优惠券追溯展示字段 */
    private AssetTracePresentation trace;
    /** 用户端优惠券生命周期时间线 */
    private List<AppCouponTimelineEventVO> timeline;
}
