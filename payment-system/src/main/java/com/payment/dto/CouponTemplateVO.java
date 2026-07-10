package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板视图对象，用于返回优惠券模板的完整信息（Admin 和 Merchant 共用）。
 */
@Data
public class CouponTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模板 ID */
    private Long id;
    /** 模板编号（业务唯一标识） */
    private String templateNo;
    /** 所属商户租户 ID */
    private Long tenantId;
    /** 模板作用范围（如 GLOBAL、PRODUCT、CATEGORY） */
    private String templateScope;
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
    /** 总库存数量 */
    private Integer totalQuantity;
    /** 已领取数量 */
    private Integer receivedQuantity;
    /** 已使用数量 */
    private Integer usedQuantity;
    /** 每人限领数量 */
    private Integer perUserLimit;
    /** 领取开始时间 */
    private LocalDateTime receiveStartTime;
    /** 领取结束时间 */
    private LocalDateTime receiveEndTime;
    /** 有效期类型（FIXED-固定日期、DYNAMIC-领取后 N 天） */
    private String validType;
    /** 领取后有效天数（动态有效期） */
    private Integer validDays;
    /** 有效期开始时间（固定有效期） */
    private LocalDateTime validStartTime;
    /** 有效期结束时间（固定有效期） */
    private LocalDateTime validEndTime;
    /** 是否可与余额叠加使用 */
    private Boolean canStackBalance;
    /** 是否可与积分叠加使用 */
    private Boolean canStackPoints;
    /** 是否可与其他优惠券叠加使用 */
    private Boolean canStackOtherCoupon;
    /** 最低可用会员等级，空表示不限 */
    private Integer requiredMemberLevel;
    /** 必须具备的会员标签 ID，逗号或 JSON 数组格式 */
    private String requiredMemberTagIds;
    /** 命中后不可用的会员标签 ID，逗号或 JSON 数组格式 */
    private String excludedMemberTagIds;
    /** 适用商品范围类型 */
    private String applicableProductScope;
    /** 适用商品范围 JSON（商品 ID 列表或分类列表） */
    private String applicableProductJson;
    /** 优惠券描述/使用说明 */
    private String description;
    /** 模板状态（如 DRAFT-草稿、ACTIVE-生效中、DISABLED-已禁用） */
    private String status;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
