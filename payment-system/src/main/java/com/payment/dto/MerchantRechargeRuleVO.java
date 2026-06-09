package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家充值规则视图对象。
 */
@Data
public class MerchantRechargeRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private BigDecimal rechargeAmount;
    private BigDecimal giftAmount;
    private Integer giftPoints;
    private Integer status;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
