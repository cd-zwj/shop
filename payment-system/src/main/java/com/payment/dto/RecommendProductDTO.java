package com.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 推荐商品DTO
 */
@Data
public class RecommendProductDTO {
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 价格
     */
    private BigDecimal price;
    
    /**
     * 图片URL
     */
    private String imageUrl;
    
    /**
     * 商家名称
     */
    private String merchantName;
    
    /**
     * 推荐分数
     */
    private Double score;
    
    /**
     * 推荐理由
     */
    private String reason;
    
    /**
     * 相似度（用于相似商品推荐）
     */
    private Double similarity;
}
