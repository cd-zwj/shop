package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 营销活动规则视图对象，展示单条活动规则的完整配置。
 */
@Data
public class ActivityRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 规则 ID */
    private Long id;

    /** 关联的活动 ID */
    private Long activityId;

    /** 规则类型 */
    private String ruleType;

    /** 满减门槛金额（元） */
    private BigDecimal thresholdAmount;

    /** 优惠金额（元） */
    private BigDecimal discountAmount;

    /** 折扣率 */
    private BigDecimal discountRate;

    /** 关联商品 ID */
    private Long productId;

    /** 关联分类编码 */
    private String categoryCode;

    /** 扩展规则配置（JSON 字符串） */
    private String ruleConfigJson;

    /** 规则优先级 */
    private Integer priority;
}
