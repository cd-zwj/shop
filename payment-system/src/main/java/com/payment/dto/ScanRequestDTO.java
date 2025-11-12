package com.payment.dto;

import lombok.Data;

/**
 * 扫码请求DTO
 */
@Data
public class ScanRequestDTO {
    private String action; // SCAN, CHECKOUT, CLEAR
    private String tenantCode;
    private String deviceId;
    private String sessionId;
    private String productCode; // 商品编码（条码）
    private Integer quantity; // 数量，默认1
}

