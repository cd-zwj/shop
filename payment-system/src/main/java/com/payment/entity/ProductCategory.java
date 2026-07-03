package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分类实体，对应数据库表 product_category。
 * <p>支持多级树形分类结构，通过 parent_id 实现父子层级关系。
 * 租户ID为NULL时表示平台级分类，非NULL时为商户自定义分类。</p>
 */
@Data
@TableName("product_category")
public class ProductCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 分类主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID，NULL表示平台级分类，非NULL表示商户自定义分类
     */
    private Long tenantId;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类ID，0表示顶级分类
     */
    private Long parentId;

    /**
     * 排序值，数值越小越靠前
     */
    private Integer sortOrder;

    /**
     * 分类图标URL
     */
    private String icon;

    /**
     * 分类状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 逻辑删除标记：0-未删除，1-已删除
     */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
