package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户优惠券实体，对应数据库表 user_coupon。
 * <p>
 * 表示用户实际持有的一张优惠券实例，由模板（coupon_template）领取生成。
 * 遵循"领取 -> 可用 -> 锁定 -> 核销/释放 -> 过期"的生命周期状态流转，
 * 使用乐观锁（version）保障并发安全。
 * </p>
 */
@Data
@TableName("user_coupon")
public class UserCoupon implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 优惠券编号，业务唯一标识 */
    private String couponNo;

    /** 关联的优惠券模板ID，对应 coupon_template 表 */
    private Long templateId;

    /** 所属租户ID，多租户行级隔离 */
    private Long tenantId;

    /** 持有用户ID，对应 platform_user 表 */
    private Long platformUserId;

    /**
     * 来源类型。
     * USER_RECEIVE-用户主动领取、SYSTEM_GIVE-系统发放、ACTIVITY-活动赠送
     */
    private String sourceType;

    /** 来源业务编号，如活动编号、发放批次号等 */
    private String sourceBizNo;

    /**
     * 优惠券状态。
     * AVAILABLE-可用、LOCKED-已锁定（下单中）、USED-已使用（核销）、EXPIRED-已过期、VOID-已作废
     */
    private String couponStatus;

    /** 当前锁定的订单编号，状态为 LOCKED 时有值 */
    private String orderNo;

    /** 锁定时间，下单时写入 */
    private LocalDateTime lockTime;

    /** 核销使用时间，支付成功时写入 */
    private LocalDateTime useTime;

    /** 优惠券过期时间，到达后不可再使用 */
    private LocalDateTime expireTime;

    /** 乐观锁版本号，防止并发扣减/状态变更冲突 */
    @Version
    private Integer version;

    /** 领取时间 */
    private LocalDateTime receiveTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
