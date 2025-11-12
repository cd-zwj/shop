package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品库存实体
 */
@Data
@TableName("product_stock")
public class ProductStock implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 库存数量
     */
    private Integer quantity;
    
    /**
     * 版本号（乐观锁）
     */
    private Integer version;
    
    private LocalDateTime updateTime;
}

