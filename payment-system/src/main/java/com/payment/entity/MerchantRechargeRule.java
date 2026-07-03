package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户充值规则实体，对应数据库表 merchant_recharge_rule。
 * <p>
 * 定义商户端钱包的充值档位配置，包含充值金额、赠送金额、赠送积分等营销激励参数。
 * 用户充值时按档位获得对应的赠送奖励，属于租户级业务数据。
 * </p>
 */
@Data
@TableName("merchant_recharge_rule")
public class MerchantRechargeRule implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID，标识该充值规则所属的商户，用于多租户数据隔离 */
    private Long tenantId;

    /** 充值金额，该档位要求用户实际支付的金额 */
    private BigDecimal rechargeAmount;

    /** 赠送金额，充值成功后额外赠送至钱包的金额；为 0 表示无赠送 */
    private BigDecimal giftAmount;

    /** 赠送积分，充值成功后额外奖励的积分数量；为 0 表示无赠送 */
    private Integer giftPoints;

    /** 状态：0-禁用，1-启用 */
    private Integer status;

    /** 排序权重，数值越小越靠前展示 */
    private Integer sortOrder;

    /** 创建时间，由数据库自动生成 */
    private LocalDateTime createTime;

    /** 更新时间，记录最后一次修改时间 */
    private LocalDateTime updateTime;
}
