package com.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商户端充值规则创建/更新请求参数。
 */
@Data
public class V1MerchantRechargeRuleDTO {

    /** 充值规则 ID（更新时必填，新建时为空） */
    private Long id;

    /** 充值金额（元） */
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于 0")
    private BigDecimal rechargeAmount;

    /** 赠送金额（元） */
    @NotNull(message = "赠送金额不能为空")
    @DecimalMin(value = "0.00", message = "赠送金额不能小于 0")
    private BigDecimal giftAmount;

    /** 赠送积分 */
    @NotNull(message = "赠送积分不能为空")
    private Integer giftPoints;

    /** 是否启用 */
    @NotNull(message = "状态不能为空")
    private Boolean enabled;

    /** 排序权重（值越小越靠前） */
    private Integer sortOrder;
}
