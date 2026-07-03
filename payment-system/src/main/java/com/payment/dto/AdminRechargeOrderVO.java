package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台管理端充值订单视图对象，展示全平台充值记录概要信息。
 */
@Data
public class AdminRechargeOrderVO {

    /** 充值订单ID */
    private Long id;

    /** 充值单号 */
    private String rechargeNo;

    /** 钱包类型（UNIFIED / MERCHANT） */
    private String walletType;

    /** 所属租户（商户）ID */
    private Long tenantId;

    /** 充值用户ID */
    private Long platformUserId;

    /** 充值金额 */
    private BigDecimal rechargeAmount;

    /** 赠送金额 */
    private BigDecimal giftAmount;

    /** 赠送积分 */
    private Integer giftPoints;

    /** 实际到账金额（充值金额 + 赠送金额） */
    private BigDecimal actualCreditAmount;

    /** 业务状态（PENDING / SUCCESS / FAILED） */
    private String bizStatus;

    /** 创建时间 */
    private LocalDateTime createTime;
}
