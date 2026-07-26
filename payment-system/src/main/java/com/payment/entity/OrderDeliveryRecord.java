package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单交付记录，对应数据库表 order_delivery_record。
 * <p>
 * payload 仅保存到店自提凭证，例如 {"pickupCode":"12345678","storeId":1}。
 */
@Data
@TableName("order_delivery_record")
public class OrderDeliveryRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID，多租户行级隔离字段 */
    private Long tenantId;

    /** 所属订单 ID，关联 sales_order.id */
    private Long orderId;

    /** 所属订单编号，冗余字段便于查询 */
    private String orderNo;

    /** 所属订单明细项 ID，关联 sales_order_item.id */
    private Long orderItemId;

    /** 交付目标用户 ID，关联 platform_user 表 */
    private Long platformUserId;

    /** 商品 ID，关联 product 表 */
    private Long productId;

    /** 商品名称快照，便于用户侧已购列表展示历史商品 */
    private String productName;

    /** 状态：PENDING / DELIVERED / CONFIRMED / REVOKED / FAILED */
    private String status;

    /** JSON 到店自提凭证 */
    private String payload;

    /** 取货码 SHA-256 哈希（hex），核销校验与同租户唯一性依据 */
    private String pickupCodeHash;

    /** 自提门店 ID */
    private Long storeId;

    /** 核销人（平台用户 ID），状态变为 CONFIRMED 时记录 */
    private Long verifiedBy;

    /** 交付失败原因，交付状态为 FAILED 时记录具体失败信息 */
    private String failReason;

    /** 交付重试次数，每次重试递增 */
    private Integer retryCount;

    /** 交付完成时间，状态变为 DELIVERED 时记录 */
    private LocalDateTime deliveredTime;

    /** 用户确认收货时间，状态变为 CONFIRMED 时记录 */
    private LocalDateTime confirmedTime;

    /** 交付撤销时间，状态变为 REVOKED 时记录（如退款后回收权益） */
    private LocalDateTime revokedTime;

    /** 逻辑删除标记，0-未删除，1-已删除 */
    @TableLogic
    private Integer deleted;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}
