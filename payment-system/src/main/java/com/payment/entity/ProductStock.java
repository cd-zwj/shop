package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品库存实体，对应数据库表 product_stock。
 * <p>记录各商品的可售库存数量，通过乐观锁（version字段）保障库存扣减的并发安全。
 * 与 product 表通过 product_id 一对一关联。</p>
 */
@Data
@TableName("product_stock")
public class ProductStock implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 库存记录主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID，用于多租户数据隔离 */
    private Long tenantId;

    /** 关联的商品ID，对应 product 表主键 */
    private Long productId;

    /** 当前可售库存数量，非负整数 */
    private Integer quantity;

    /**
     * 乐观锁版本号，每次库存扣减/回滚时自增。
     * 用于防止并发超卖，资金安全相关字段。
     */
    @Version
    private Integer version;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
