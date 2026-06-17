package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sales_order_item")
public class SalesOrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long tenantId;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;

    /**
     * 冗余商品类型，避免商品后续改类型时影响历史订单的交付路由。
     */
    private String productType;

    /**
     * 交付状态：PENDING / DELIVERING / DELIVERED / CONFIRMED / FAILED / REVOKED
     */
    private String deliveryStatus;

    /**
     * 交付完成时间
     */
    private LocalDateTime deliveredTime;

    private LocalDateTime createTime;
}
