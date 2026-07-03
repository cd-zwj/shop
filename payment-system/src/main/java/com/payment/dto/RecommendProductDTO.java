package com.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 推荐商品视图对象，用于返回个性化推荐或相似商品推荐结果。
 */
@Data
public class RecommendProductDTO {
    /** 商品 ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品价格 */
    private BigDecimal price;

    /** 商品图片 URL */
    private String imageUrl;

    /** 所属商家名称 */
    private String merchantName;

    /** 推荐分数（值越高推荐度越高） */
    private Double score;

    /** 推荐理由（如 "您浏览过的相似商品"） */
    private String reason;

    /** 相似度（用于相似商品推荐场景，取值 0-1） */
    private Double similarity;
}
