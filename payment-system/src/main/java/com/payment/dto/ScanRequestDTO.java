package com.payment.dto;

import lombok.Data;

/**
 * 扫码操作请求数据传输对象，用于收银终端扫码枪场景（扫码、结账、清空购物车）。
 */
@Data
public class ScanRequestDTO {
    /** 操作类型：SCAN（扫码添加商品）、CHECKOUT（结账）、CLEAR（清空购物车） */
    private String action;
    /** 商户编码 */
    private String tenantCode;
    /** 设备 ID */
    private String deviceId;
    /** 会话 ID（同一次收银会话） */
    private String sessionId;
    /** 商品编码（条形码/二维码） */
    private String productCode;
    /** 扫码数量，默认 1 */
    private Integer quantity;
}

