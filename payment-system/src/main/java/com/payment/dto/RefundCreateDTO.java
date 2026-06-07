package com.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户提交退款申请请求体。
 */
@Data
public class RefundCreateDTO {

    /** 关联订单号 */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /** 关联订单项ID，可选——部分退款时用 */
    private Long orderItemId;

    /** 退款类型：REFUND_ONLY / RETURN_REFUND */
    @NotBlank(message = "退款类型不能为空")
    private String refundType;

    /** 退款金额（元） */
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    private BigDecimal refundAmount;

    /** 退款原因 */
    @NotBlank(message = "退款原因不能为空")
    @Size(max = 200, message = "退款原因最多200字")
    private String reason;

    /** 详细描述 */
    @Size(max = 500, message = "详细描述最多500字")
    private String description;
}
