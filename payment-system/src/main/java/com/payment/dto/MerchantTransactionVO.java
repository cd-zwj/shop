package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商户收支流水记录。
 */
@Data
public class MerchantTransactionVO {

    private Long id;

    /** 业务类型：PAYMENT / REFUND / RECHARGE / WITHDRAWAL / POINTS 等。 */
    private String bizType;

    /** 关联业务单号。 */
    private String bizNo;

    /** 变动金额（正为收入，负为支出）。 */
    private BigDecimal changeAmount;

    /** 变动前余额。 */
    private BigDecimal balanceBefore;

    /** 变动后余额。 */
    private BigDecimal balanceAfter;

    /** 备注。 */
    private String remark;

    /** 创建时间。 */
    private String createTime;
}
