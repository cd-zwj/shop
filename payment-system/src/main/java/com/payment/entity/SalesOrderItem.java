package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单明细表，对应数据库表 sales_order_item。
 * <p>
 * 记录销售订单中每个商品项的购买信息与交付状态。
 * 一条销售订单可包含多个商品项，每项独立跟踪交付进度。
 * </p>
 */
@Data
@TableName("sales_order_item")
public class SalesOrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属销售订单 ID，关联 sales_order.id */
    private Long orderId;

    /** 所属销售订单编号，冗余字段便于查询 */
    private String orderNo;

    /** 租户 ID，多租户行级隔离字段 */
    private Long tenantId;

    /** 商品 ID，关联 product 表 */
    private Long productId;

    /** 商品名称，下单时快照冗余，不受后续商品改名影响 */
    private String productName;

    /** 商品单价，下单时快照，单位：元 */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 小计金额 = price x quantity，单位：元 */
    private BigDecimal subtotal;

    /**
     * 交付状态：PENDING / DELIVERING / DELIVERED / CONFIRMED / FAILED / REVOKED
     */
    private String deliveryStatus;

    /**
     * 交付完成时间
     */
    private LocalDateTime deliveredTime;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
