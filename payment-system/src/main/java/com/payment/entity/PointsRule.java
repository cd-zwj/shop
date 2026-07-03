package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分规则实体，对应数据库表 points_rule。
 * 定义租户下各类积分发放规则，如支付送积分、签到送积分、分享送积分等。
 * 系统根据规则类型自动计算并发放积分。
 */
@Data
@TableName("points_rule")
public class PointsRule implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID，多租户隔离标识
     */
    private Long tenantId;

    /**
     * 规则名称，如"消费送积分"、"每日签到"
     */
    private String ruleName;

    /**
     * 规则类型：PAYMENT-支付获得，SIGNIN-签到，SHARE-分享
     */
    private String ruleType;

    /**
     * 每次触发规则发放的积分数量
     */
    private Integer pointsAmount;

    /**
     * 条件金额，如支付满多少元才触发积分发放，仅对 PAYMENT 类型生效
     */
    private BigDecimal conditionAmount;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
