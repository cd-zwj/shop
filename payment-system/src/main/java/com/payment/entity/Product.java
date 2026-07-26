package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体，对应数据库表 product。
 * <p>存储实体商品基本信息。
 * 通过 tenant_id 实现多租户数据隔离。门店售价、上下架状态和库存由
 * {@code store_product} 与 {@code store_product_stock} 管理。</p>
 */
@Data
@TableName("product")
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 商品主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID，用于多租户数据隔离
     */
    private Long tenantId;

    /**
     * 商品编码，全局唯一标识（如条码、SKU编号）
     */
    private String productCode;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品单价，精确到分
     */
    private BigDecimal price;

    /**
     * 计量单位（如：件、个、次、月）
     */
    private String unit;

    /**
     * 商品分类名称（冗余字段，便于查询展示）
     */
    private String category;

    /**
     * 商品图片URL，存储于OSS/MinIO
     */
    private String imageUrl;

    /**
     * 商品详细描述，支持富文本
     */
    private String description;

    /** 当前目录查询所选门店，仅作为联表结果承载，不映射 product 表。 */
    @TableField(exist = false)
    private Long storeId;

    /** 当前仅支持到店自提，仅作为目录查询结果承载，不映射 product 表。 */
    @TableField(exist = false)
    private String fulfillmentMode;

    /** 当前门店可售库存，仅用于目录查询结果承载。 */
    @TableField(exist = false)
    private Integer stock;

    /**
     * 商品状态：0-下架，1-上架
     */
    private Integer status;

    /** 逻辑删除标记：0-未删除，1-已删除 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}

