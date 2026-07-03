package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 优惠券作用范围实体，对应数据库表 coupon_scope。
 * <p>
 * 定义优惠券可适用的具体商品、商品分类或其他维度，
 * 与 coupon_template 为一对多关系，支持一张模板关联多个作用范围条目。
 * </p>
 */
@Data
@TableName("coupon_scope")
public class CouponScope implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的优惠券模板ID，对应 coupon_template 表 */
    private Long couponTemplateId;

    /**
     * 范围类型。
     * PRODUCT-指定商品、CATEGORY-指定商品分类、BRAND-指定品牌
     */
    private String scopeType;

    /** 范围对象ID，如商品ID或分类ID */
    private Long scopeId;

    /** 范围对象编码，冗余字段便于快速匹配 */
    private String scopeCode;

    /** 所属租户ID，多租户行级隔离 */
    private Long tenantId;

    /** 逻辑删除标记，0-未删除、1-已删除 */
    private Integer deleted;
}
