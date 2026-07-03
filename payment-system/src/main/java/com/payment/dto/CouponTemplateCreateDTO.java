package com.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板创建数据传输对象，用于商户或平台管理员创建新的优惠券模板。
 */
@Data
public class CouponTemplateCreateDTO {
    /** 所属商户租户 ID（平台创建时可指定，商户创建时自动填充） */
    private Long tenantId;
    /** 模板作用范围（如 GLOBAL-全场通用、PRODUCT-指定商品、CATEGORY-指定分类） */
    private String templateScope;
    /** 优惠券名称 */
    @NotBlank(message = "优惠券名称不能为空")
    @Size(max = 100, message = "名称长度不能超过100")
    private String templateName;
    /** 优惠券类型（如 FIXED-满减、DISCOUNT-折扣） */
    @NotBlank(message = "优惠券类型不能为空")
    private String couponType;
    /** 使用门槛金额（满 X 元可用） */
    private BigDecimal thresholdAmount;
    /** 固定减免金额（满减券） */
    private BigDecimal discountAmount;
    /** 折扣率（折扣券，如 0.85 表示 85 折） */
    private BigDecimal discountRate;
    /** 最大减免金额（折扣券封顶值） */
    private BigDecimal maxDiscountAmount;
    /** 总库存数量 */
    @NotNull(message = "库存不能为空")
    @Min(value = 1, message = "库存必须大于0")
    private Integer totalQuantity;
    /** 每人限领数量 */
    @Min(value = 1, message = "每人限领数必须大于0")
    private Integer perUserLimit;
    /** 领取开始时间 */
    private LocalDateTime receiveStartTime;
    /** 领取结束时间 */
    private LocalDateTime receiveEndTime;
    /** 领取后有效天数（动态有效期类型） */
    private Integer validDays;
    /** 有效期开始时间（固定有效期类型） */
    private LocalDateTime validStartTime;
    /** 有效期结束时间（固定有效期类型） */
    private LocalDateTime validEndTime;
    /** 优惠券描述/使用说明 */
    private String description;
}
