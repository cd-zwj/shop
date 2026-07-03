package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值规则实体。
 * 对应数据库表 recharge_rule，定义商户可配置的钱包充值档位规则。
 * <p>每条规则表示一个充值档位：用户充值 rechargeAmount 金额后，
 * 系统额外赠送 giftAmount 金额和 giftPoints 积分。
 * <p>商户可创建多个充值档位，通过 sortOrder 控制展示顺序，通过 status 控制是否启用。
 */
@Data
@TableName("recharge_rule")
public class RechargeRule implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID（商户），用于多租户隔离 */
    private Long tenantId;

    /** 充值金额（用户需支付的金额），精确到分 */
    private BigDecimal rechargeAmount;

    /** 赠送金额，充值成功后额外赠送到钱包的金额，精确到分 */
    private BigDecimal giftAmount;

    /** 赠送积分，充值成功后额外赠送的积分数量 */
    private Integer giftPoints;

    /**
     * 规则状态。
     * 0=禁用（不在前端展示），1=启用（用户可见可选）
     */
    private Integer status;

    /** 排序序号，数值越小越靠前展示 */
    private Integer sortOrder;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}
