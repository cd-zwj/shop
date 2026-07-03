package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户充值规则视图对象，展示单条充值档位的详细配置。
 */
@Data
public class MerchantRechargeRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 充值规则 ID */
    private Long id;

    /** 所属租户（商户）ID */
    private Long tenantId;

    /** 充值金额（元） */
    private BigDecimal rechargeAmount;

    /** 赠送金额（元） */
    private BigDecimal giftAmount;

    /** 赠送积分 */
    private Integer giftPoints;

    /** 启用状态（0-禁用，1-启用） */
    private Integer status;

    /** 排序权重（值越小越靠前） */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
