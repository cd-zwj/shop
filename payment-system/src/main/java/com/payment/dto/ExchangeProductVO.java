package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分兑换商品视图对象，展示可兑换商品的完整信息。
 */
@Data
public class ExchangeProductVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 兑换商品 ID */
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 商品名称 */
    private String productName;

    /** 商品图片 URL */
    private String productImage;

    /** 兑换所需积分 */
    private Integer pointsRequired;

    /** 兑换库存 */
    private Integer stock;

    /** 每人兑换限制（为 null 表示不限制） */
    private Integer exchangeLimit;

    /** 商品描述 */
    private String description;

    /** 上架状态（0-下架，1-上架） */
    private Integer status;

    /** 排序权重（值越小越靠前） */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
