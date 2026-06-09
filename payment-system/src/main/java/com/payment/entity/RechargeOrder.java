package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值订单实体
 */
@Data
@TableName("recharge_order")
public class RechargeOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 充值订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 充值金额
     */
    private BigDecimal rechargeAmount;

    /**
     * 赠送金额
     */
    private BigDecimal giftAmount;

    /**
     * 赠送积分
     */
    private Integer giftPoints;

    /**
     * 实际到账金额
     */
    private BigDecimal actualAmount;

    /**
     * 支付方式：WECHAT-微信，ALIPAY-支付宝
     */
    private String payType;

    /**
     * 支付状态：PENDING-待支付，SUCCESS-成功，FAIL-失败
     */
    private String payStatus;

    /**
     * 第三方订单号
     */
    private String thirdPartyOrderNo;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
