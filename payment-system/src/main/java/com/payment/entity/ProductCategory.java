package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分类实体
 */
@Data
@TableName("product_category")
public class ProductCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID（NULL表示平台级分类）
     */
    private Long tenantId;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类ID，0表示顶级
     */
    private Long parentId;

    /**
     * 排序值
     */
    private Integer sortOrder;

    /**
     * 分类图标
     */
    private String icon;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 逻辑删除: 0-否, 1-是
     */
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
