package com.payment.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 创建订单数据传输对象，用于通用下单场景（支持微信/支付宝/余额/混合支付）。
 */
@Data
public class CreateOrderDTO {
    /** 订单金额 */
    @NotNull(message = "订单金额不能为空")
    private BigDecimal amount;

    /** 支付方式：WECHAT / ALIPAY / BALANCE / MIXED */
    @NotBlank(message = "支付方式不能为空")
    private String payType;

    /** 订单标题（展示在支付页面） */
    @NotBlank(message = "订单标题不能为空")
    private String subject;

    /** 订单描述/正文 */
    private String body;

    /** 异步回调通知地址 */
    private String notifyUrl;

    /** 使用余额支付的金额（仅当 payType 为 BALANCE 或 MIXED 时有效） */
    private BigDecimal balanceAmount;

    /** 是否使用余额支付 */
    private Boolean useBalance;
}

