package com.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 支付响应DTO
 */
@Data
public class PayResponseDTO {
    private String orderNo;
    private String payType;
    private BigDecimal amount;
    private String payUrl; // 支付二维码URL或支付链接
    private String qrCode; // 二维码内容
}

