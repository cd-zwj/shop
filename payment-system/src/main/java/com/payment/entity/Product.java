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
 * <p>存储商品基本信息，支持实物、虚拟、卡密、服务、订阅等多种商品类型。
 * 通过 tenant_id 实现多租户数据隔离，通过 store_id 关联所属门店。</p>
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

    /**
     * 所属门店ID，关联 store 表
     */
    private Long storeId;

    /** 虚拟商品类型 ID，关联 virtual_product_type。 */
    private Long virtualTypeId;

    /** 虚拟商品分类 ID，关联 virtual_product_category。 */
    private Long virtualCategoryId;

    /** 履约形态：ONLINE_VIRTUAL / OFFLINE_SERVICE / EXPRESS_DELIVERY。 */
    private String fulfillmentMode;

    /**
     * 商品类型：PHYSICAL-实物 / VIRTUAL-虚拟 / CARD_KEY-卡密 / SERVICE-服务 / SUBSCRIPTION-订阅。
     * <p>决定支付成功后走哪一种交付策略。</p>
     */
    private String productType;

    /**
     * 交付配置（JSON格式），按 productType 解读：
     * <ul>
     *   <li>VIRTUAL = {"contentUrl":"...","accountInfo":"..."}</li>
     *   <li>CARD_KEY = 使用 card_key_pool 库存池上传和锁定卡密</li>
     *   <li>SERVICE = 可留空，支付后系统生成核销码</li>
     *   <li>SUBSCRIPTION = {"validityDays":30}</li>
     * </ul>
     */
    private String deliveryConfig;

    /** 当前可售库存，仅用于用户端详情等联表查询结果承载。 */
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

