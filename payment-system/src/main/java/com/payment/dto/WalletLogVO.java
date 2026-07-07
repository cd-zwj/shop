package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包流水日志视图对象，用于返回钱包余额的收支明细。
 */
@Data
public class WalletLogVO {
    /** 钱包类型（如 UNIFIED-统一钱包、MERCHANT-商户钱包） */
    private String walletType;
    /** 所属商户租户 ID */
    private Long tenantId;
    /** 业务类型（如 RECHARGE-充值、CONSUME-消费、REFUND-退款） */
    private String bizType;
    /** 关联业务单号 */
    private String bizNo;
    /** 变更金额（正数为收入，负数为支出） */
    private BigDecimal changeAmount;
    /** 变更前余额 */
    private BigDecimal balanceBefore;
    /** 变更后余额 */
    private BigDecimal balanceAfter;
    /** 备注说明 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 用户端资产追溯展示字段 */
    private AssetTracePresentation trace;

    public void attachTrace() {
        this.trace = AssetTracePresentations.wallet(this);
    }
}
