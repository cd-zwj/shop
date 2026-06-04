package com.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板创建参数。
 */
@Data
public class CouponTemplateCreateDTO {
    private Long tenantId;
    private String ownerType;
    @NotBlank(message = "优惠券名称不能为空")
    @Size(max = 100, message = "名称长度不能超过100")
    private String name;
    @NotBlank(message = "优惠券类型不能为空")
    private String couponType;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscountAmount;
    @NotNull(message = "库存不能为空")
    @Min(value = 1, message = "库存必须大于0")
    private Integer totalStock;
    @Min(value = 1, message = "每人限领数必须大于0")
    private Integer perUserLimit;
    private LocalDateTime receiveStartTime;
    private LocalDateTime receiveEndTime;
    private Integer validDaysAfterReceive;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private Integer minMemberLevel;
    private String excludeMemberTagIds;
    private String stackStrategy;
    private String description;
}
