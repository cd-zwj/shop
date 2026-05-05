package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品库存实体。
 */
@Data
@TableName("product_stock")
public class ProductStock implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long productId;

    private Integer quantity;

    /**
     * 乐观锁版本。
     */
    @Version
    private Integer version;

    private LocalDateTime updateTime;
}
