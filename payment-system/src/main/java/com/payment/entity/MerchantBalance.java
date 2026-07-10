package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户余额实体
 * <p>对应数据库表 merchant_balance，记录每个租户（商户）的综合余额信息。
 * 与商户钱包（merchant_wallet_account）不同，此处记录的是商户维度的整体资金概况，
 * 包含可用余额、冻结余额、累计收入和累计提现。余额操作使用乐观锁 {@code @Version} 防止并发不一致。</p>
 */
@Data
@TableName("merchant_balance")
public class MerchantBalance implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户 ID，标识所属商户，用于多租户数据隔离
     */
    private Long tenantId;

    /**
     * 可用余额（元），商户当前可提现或使用的金额
     */
    private BigDecimal balance;

    /**
     * 冻结余额（元），因退款审核、提现处理等原因被临时冻结的金额
     */
    private BigDecimal frozenBalance;

    /**
     * 累计收入（元），商户历史总收入金额，仅增不减
     */
    private BigDecimal totalIncome;

    /**
     * 累计提现（元），商户历史总提现金额，仅增不减
     */
    private BigDecimal totalWithdrawal;

    /**
     * 累计平台服务费（元），订单结算时按平台抽成规则累计
     */
    private BigDecimal totalPlatformFee;

    /**
     * 逻辑删除标记：0-未删除，1-已删除
     */
    private Integer deleted;

    /**
     * 乐观锁版本号，用于余额并发操作的安全保障，每次更新自动 +1
     */
    @Version
    private Integer version;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;
}
