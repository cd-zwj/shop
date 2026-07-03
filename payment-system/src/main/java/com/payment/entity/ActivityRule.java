package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 营销活动规则实体，对应数据库表 activity_rule。
 * <p>
 * 描述某一次营销活动下的具体规则条目，如满减门槛、折扣比例、指定商品/品类优惠等。
 * 一条活动可关联多条规则，通过 priority 控制规则优先级。
 * </p>
 */
@Data
@TableName("activity_rule")
public class ActivityRule implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属营销活动ID，关联 activity 表 */
    private Long activityId;

    /**
     * 规则类型，如 FULL_REDUCTION（满减）、DISCOUNT（折扣）、GIFT（赠品）等
     */
    private String ruleType;

    /**
     * 满减/满赠门槛金额，满足该金额条件后触发优惠
     */
    private BigDecimal thresholdAmount;

    /** 优惠金额，满减时为减免的固定金额 */
    private BigDecimal discountAmount;

    /** 折扣比例，折扣类型规则时使用，取值范围 0.01 ~ 1.00（如 0.85 表示 85 折） */
    private BigDecimal discountRate;

    /** 指定优惠商品ID，关联 product 表；为空表示不限定商品 */
    private Long productId;

    /** 指定优惠品类编码；为空表示不限定品类 */
    private String categoryCode;

    /** 扩展规则配置，JSON 格式存储复杂的自定义规则参数 */
    private String ruleConfigJson;

    /** 规则优先级，数值越小优先级越高，同一活动内多条规则按此排序匹配 */
    private Integer priority;

    /** 逻辑删除标志：0-未删除，1-已删除 */
    private Integer deleted;
}
