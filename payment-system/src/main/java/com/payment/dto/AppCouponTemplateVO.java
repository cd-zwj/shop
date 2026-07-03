package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户端可领取优惠券模板视图对象，用于展示优惠券详情及领取状态。
 */
@Data
public class AppCouponTemplateVO {
    /** 优惠券模板 ID */
    private Long id;
    /** 所属商户租户 ID */
    private Long tenantId;
    /** 模板作用范围（如 GLOBAL-全场通用、PRODUCT-指定商品、CATEGORY-指定分类） */
    private String templateScope;
    /** 优惠券名称 */
    private String templateName;
    /** 优惠券类型（如 FIXED-满减、DISCOUNT-折扣） */
    private String couponType;
    /** 使用门槛金额（满 X 元可用） */
    private BigDecimal thresholdAmount;
    /** 固定减免金额（满减券） */
    private BigDecimal discountAmount;
    /** 折扣率（折扣券，如 0.85 表示 85 折） */
    private BigDecimal discountRate;
    /** 最大减免金额（折扣券封顶值） */
    private BigDecimal maxDiscountAmount;
    /** 每人限领数量 */
    private Integer perUserLimit;
    /** 剩余可领取库存 */
    private Integer remainingStock;
    /** 当前用户已领取数量 */
    private Integer receivedByCurrentUser;
    /** 当前用户是否可领取 */
    private Boolean receivable;
    /** 领取开始时间 */
    private LocalDateTime receiveStartTime;
    /** 领取结束时间 */
    private LocalDateTime receiveEndTime;
    /** 有效期开始时间（固定有效期类型） */
    private LocalDateTime validStartTime;
    /** 有效期结束时间（固定有效期类型） */
    private LocalDateTime validEndTime;
    /** 领取后有效天数（动态有效期类型） */
    private Integer validDays;
    /** 优惠券描述/使用说明 */
    private String description;
}
