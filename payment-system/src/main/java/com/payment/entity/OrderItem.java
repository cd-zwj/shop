package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单商品明细实体，对应数据库表 order_item。
 * <p>
 * 记录订单中每个商品项的快照信息，包括商品编码、名称、图片、单价、数量等。
 * 属于 PaymentOrder 体系（旧版订单），与 SalesOrderItem 属于不同订单模型。
 * </p>
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID，多租户行级隔离字段 */
    private Long tenantId;

    /** 所属订单 ID，关联 payment_order.id */
    private Long orderId;

    /** 所属订单编号，冗余字段便于查询 */
    private String orderNo;

    /** 商品 ID，关联 product 表 */
    private Long productId;

    /** 商品编码，下单时快照 */
    private String productCode;

    /** 商品名称，下单时快照 */
    private String productName;

    /** 商品图片 URL，下单时快照 */
    private String productImage;

    /** 商品单价，下单时快照，单位：元 */
    private BigDecimal price;

    /** 购买数量 */
    private Integer quantity;

    /** 小计金额 = price x quantity，单位：元 */
    private BigDecimal subtotal;

    /** 记录创建时间 */
    private LocalDateTime createTime;
}
