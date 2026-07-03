package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券锁定记录实体，对应数据库表 coupon_lock_record。
 * <p>
 * 用户下单时对优惠券进行预锁定，防止并发场景下同一优惠券被重复使用。
 * 锁定后若订单支付成功则转为核销，若订单取消或超时则释放。
 * </p>
 */
@Data
@TableName("coupon_lock_record")
public class CouponLockRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的用户优惠券ID，对应 user_coupon 表 */
    private Long userCouponId;

    /** 所属租户ID，多租户行级隔离 */
    private Long tenantId;

    /** 关联的订单ID */
    private Long orderId;

    /** 关联的订单编号 */
    private String orderNo;

    /** 业务流水号，用于幂等校验和链路追踪 */
    private String bizNo;

    /** 锁定时间 */
    private LocalDateTime lockTime;

    /**
     * 锁定状态。
     * LOCKED-已锁定（等待支付）、RELEASED-已释放（订单取消/超时）、CONSUMED-已消费（支付成功转核销）
     */
    private String lockStatus;
}
