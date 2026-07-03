package com.payment.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 创建充值订单数据传输对象，用于用户端发起余额充值请求。
 */
@Data
public class CreateRechargeOrderDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 充值规则 ID（关联充值金额档位） */
    @NotNull(message = "充值规则ID不能为空")
    private Long ruleId;
}
