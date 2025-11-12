package com.payment.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 扫码响应DTO
 */
@Data
public class ScanResponseDTO {
    private String productCode;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer stock; // 库存
    private String status; // SUCCESS, NOT_FOUND, OUT_OF_STOCK, UNAVAILABLE, ERROR
    private String message;
    private Map<String, Object> cartData; // 购物车信息（总数量、总金额）
}

