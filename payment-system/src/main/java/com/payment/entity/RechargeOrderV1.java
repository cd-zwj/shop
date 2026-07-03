package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值订单，对应数据库表 recharge_order_v1。
 * <p>
 * 记录用户向钱包充值的订单信息，支持统一钱包和商户钱包两种充值类型。
 * 充值时可享受赠送金额和赠送积分等营销活动优惠。
 * </p>
 */
@Data
@TableName("recharge_order_v1")
public class RechargeOrderV1 implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 充值订单编号，全局唯一 */
    private String rechargeNo;

    /** 钱包类型，取值如：UNIFIED(统一钱包) / MERCHANT(商户钱包) */
    private String walletType;

    /** 租户 ID，多租户行级隔离字段 */
    private Long tenantId;

    /** 充值用户 ID，关联 platform_user 表 */
    private Long platformUserId;

    /** 充值规则 ID，关联充值规则表，决定充值档位和赠送策略 */
    private Long ruleId;

    /** 用户实际充值金额，单位：元 */
    private BigDecimal rechargeAmount;

    /** 赠送金额，根据充值规则赠送的额外金额，单位：元 */
    private BigDecimal giftAmount;

    /** 赠送积分，根据充值规则赠送的积分数量 */
    private Integer giftPoints;

    /** 实际到账金额 = rechargeAmount + giftAmount，单位：元 */
    private BigDecimal actualCreditAmount;

    /**
     * 业务状态，取值如：PENDING(待支付) / SUCCESS(充值成功) / FAILED(充值失败) / CANCELLED(已取消)
     */
    private String bizStatus;

    /** 逻辑删除标记，0-未删除，1-已删除 */
    private Integer deleted;

    /** 订单创建时间 */
    private LocalDateTime createTime;

    /** 订单最后更新时间 */
    private LocalDateTime updateTime;
}
