package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值订单实体（旧版）。
 * 对应数据库表 recharge_order，记录用户为钱包充值的订单信息。
 * <p>充值流程：用户选择充值规则 → 创建充值订单 → 调用支付渠道付款 → 支付成功后到账。
 * <p>实际到账金额 = 充值金额 + 赠送金额，同时可赠送积分。
 */
@Data
@TableName("recharge_order")
public class RechargeOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID（商户），用于多租户隔离 */
    private Long tenantId;

    /** 充值订单号，全局唯一 */
    private String orderNo;

    /** 充值用户ID */
    private Long userId;

    /** 充值金额（用户实际支付金额），精确到分 */
    private BigDecimal rechargeAmount;

    /** 赠送金额，根据 {@link RechargeRule} 规则计算，精确到分 */
    private BigDecimal giftAmount;

    /** 赠送积分，根据充值规则计算 */
    private Integer giftPoints;

    /** 实际到账金额 = rechargeAmount + giftAmount，精确到分 */
    private BigDecimal actualAmount;

    /**
     * 支付方式。
     * WECHAT=微信支付，ALIPAY=支付宝
     */
    private String payType;

    /**
     * 支付状态。
     * PENDING=待支付，SUCCESS=支付成功，FAIL=支付失败
     */
    private String payStatus;

    /** 第三方支付平台返回的交易单号 */
    private String thirdPartyOrderNo;

    /** 支付成功时间 */
    private LocalDateTime payTime;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}
