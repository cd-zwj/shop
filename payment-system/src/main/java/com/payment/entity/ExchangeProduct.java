package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分兑换商品实体
 */
@Data
@TableName("exchange_product")
public class ExchangeProduct implements Serializable {
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
     * 所需积分
     */
    private Integer pointsRequired;
    
    /**
     * 兑换库存
     */
    private Integer stock;
    
    /**
     * 状态（0-下架，1-上架）
     */
    private Integer status;
    
    private Integer deleted;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
