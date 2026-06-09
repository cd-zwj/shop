package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分规则实体
 */
@Data
@TableName("points_rule")
public class PointsRule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型：PAYMENT-支付获得，SIGNIN-签到，SHARE-分享
     */
    private String ruleType;

    /**
     * 积分数量
     */
    private Integer pointsAmount;

    /**
     * 条件金额（支付满多少）
     */
    private BigDecimal conditionAmount;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
