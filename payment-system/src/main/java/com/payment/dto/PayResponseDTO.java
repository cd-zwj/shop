package com.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 支付响应数据传输对象，用于返回创建支付后的支付链接和二维码信息。
 */
@Data
public class PayResponseDTO {
    /** 订单编号 */
    private String orderNo;
    /** 支付方式（如 WECHAT, ALIPAY） */
    private String payType;
    /** 支付金额 */
    private BigDecimal amount;
    /** 支付页面跳转 URL（用于 H5 支付跳转） */
    private String payUrl;
    /** 二维码内容（用于扫码支付展示） */
    private String qrCode;
}

