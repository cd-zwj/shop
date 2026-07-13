package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券释放记录实体，对应数据库表 coupon_release_record。
 * <p>
 * 记录已锁定优惠券的释放流水。当订单取消、支付超时或发生退款时，
 * 将之前锁定的优惠券释放回用户账户，使其可再次使用。
 * </p>
 */
@Data
@TableName("coupon_release_record")
public class CouponReleaseRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的用户优惠券ID，对应 user_coupon 表 */
    private Long userCouponId;

    /** 所属租户ID，多租户行级隔离 */
    private Long tenantId;

    /** 持券用户ID，用于用户维度的事件查询 */
    private Long platformUserId;

    /** 关联的订单ID */
    private Long orderId;

    /** 关联的订单编号 */
    private String orderNo;

    /** 业务流水号，用于幂等校验和链路追踪 */
    private String bizNo;

    /**
     * 释放原因。
     * ORDER_CANCEL-订单取消、PAYMENT_TIMEOUT-支付超时、REFUND-退款释放
     */
    private String releaseReason;

    /** 释放时间 */
    private LocalDateTime releaseTime;
}
