package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 */
@Data
@TableName("product")
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 商品编码（条码/唯一标识）
     */
    private String productCode;
    
    /**
     * 商品名称
     */
    private String name;
    
    /**
     * 单价
     */
    private BigDecimal price;
    
    /**
     * 单位
     */
    private String unit;
    
    /**
     * 分类
     */
    private String category;
    
    /**
     * 商品图片URL（OSS）
     */
    private String imageUrl;
    
    /**
     * 商品描述
     */
    private String description;

    /**
     * 门店ID
     */
    private Long storeId;

    /**
     * 商品类型：PHYSICAL / VIRTUAL / CARD_KEY / SERVICE / SUBSCRIPTION
     * 决定支付成功后走哪一种 DeliveryStrategy。
     */
    private String productType;

    /**
     * 交付配置(JSON)，按 productType 解读：
     * VIRTUAL = {"contentUrl":"...","accountInfo":"..."}
     * CARD_KEY = 使用 card_key_pool 库存池上传和锁定卡密。
     * SERVICE = 可留空，支付后系统生成核销码。
     * SUBSCRIPTION = {"validityDays":30}
     */
    private String deliveryConfig;

    /**
     * 状态：0-下架，1-上架
     */
    private Integer status;
    
    private Integer deleted;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}

