package com.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户端商品搜索结果卡片视图对象，用于展示搜索结果中的商品信息。
 */
@Data
public class AppCatalogSearchProductVO {

    /** 卡片类型标识，固定为 "product" */
    private String type = "product";

    /** 搜索结果记录 ID */
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** 所属商户租户 ID */
    private Long tenantId;

    /** 所属商户名称 */
    private String tenantName;

    /** 商品标题 */
    private String title;

    /** 商品名称 */
    private String name;

    /** 商品副标题 */
    private String subtitle;

    /** 商品分类 */
    private String category;

    /** 商品价格 */
    private BigDecimal price;

    /** 评分 */
    private BigDecimal rating;

    /** 距离标签（如 1.2km） */
    private String distanceLabel;

    /** 商品封面图 URL */
    private String coverImage;

    /** 商品状态（0-下架, 1-上架） */
    private Integer status;
}
