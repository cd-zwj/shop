package com.payment.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 扫码操作响应数据传输对象，用于返回扫码枪扫码后的商品信息和购物车状态。
 */
@Data
public class ScanResponseDTO {
    /** 商品编码（条形码） */
    private String productCode;
    /** 商品 ID */
    private Long productId;
    /** 商品名称 */
    private String productName;
    /** 商品图片 URL */
    private String productImage;
    /** 商品单价 */
    private BigDecimal price;
    /** 当前库存 */
    private Integer stock;
    /** 扫码状态：SUCCESS / NOT_FOUND / OUT_OF_STOCK / UNAVAILABLE / ERROR */
    private String status;
    /** 状态提示消息 */
    private String message;
    /** 购物车汇总信息（含总数量、总金额等） */
    private Map<String, Object> cartData;
}

