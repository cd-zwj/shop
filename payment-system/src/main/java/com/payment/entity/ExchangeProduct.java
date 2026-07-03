package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分兑换商品实体，对应数据库表 exchange_product。
 * <p>
 * 定义会员可用积分兑换的商品/虚拟物品，包含兑换所需积分、库存、每人兑换限制等信息。
 * 属于租户级业务数据，通过 tenant_id 进行多租户隔离。
 * </p>
 */
@Data
@TableName("exchange_product")
public class ExchangeProduct implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID，用于多租户数据隔离
     */
    private Long tenantId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品图片，存储图片 URL 地址
     */
    private String productImage;

    /**
     * 所需积分，用户兑换该商品需扣除的积分数量
     */
    private Integer pointsRequired;

    /**
     * 兑换库存，剩余可兑换数量；为 0 时不可兑换
     */
    private Integer stock;

    /**
     * 兑换限制（每人），单个用户最大兑换次数；为 0 表示不限制
     */
    private Integer exchangeLimit;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 状态（0-下架，1-上架）
     */
    private Integer status;

    /**
     * 排序权重，数值越小越靠前展示
     */
    private Integer sortOrder;

    /**
     * 是否删除：0-否，1-是
     */
    private Integer deleted;

    /** 创建时间，由数据库自动生成 */
    private LocalDateTime createTime;

    /** 更新时间，记录最后一次修改时间 */
    private LocalDateTime updateTime;
}
