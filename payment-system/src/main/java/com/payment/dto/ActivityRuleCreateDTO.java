package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 营销活动规则创建请求参数。
 */
@Data
public class ActivityRuleCreateDTO {

    /** 关联的活动 ID */
    private Long activityId;

    /** 规则类型（如：THRESHOLD_REDUCTION-满减 / DISCOUNT_RATE-折扣 / GIFT-赠品） */
    private String ruleType;

    /** 满减门槛金额（元） */
    private BigDecimal thresholdAmount;

    /** 优惠金额（元），满减场景使用 */
    private BigDecimal discountAmount;

    /** 折扣率（如 0.85 表示 85 折），折扣场景使用 */
    private BigDecimal discountRate;

    /** 关联商品 ID（商品级活动时使用） */
    private Long productId;

    /** 关联分类编码（分类级活动时使用） */
    private String categoryCode;

    /** 扩展规则配置（JSON 字符串，用于存储复杂规则） */
    private String ruleConfigJson;

    /** 规则优先级（值越小优先级越高） */
    private Integer priority;
}
