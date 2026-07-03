package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券操作日志实体，对应数据库表 coupon_operation_log。
 * <p>
 * 记录优惠券全生命周期的操作流水，包括领取、锁定、解锁、使用、过期、作废等，
 * 同时记录操作前后的状态变化，便于审计和问题排查。
 * </p>
 */
@Data
@TableName("coupon_operation_log")
public class CouponOperationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 优惠券编号，对应 user_coupon 表的 couponNo */
    private String couponNo;

    /** 操作用户ID，对应 platform_user 表 */
    private Long platformUserId;

    /** 所属租户ID，多租户行级隔离 */
    private Long tenantId;

    /** 操作类型：RECEIVE-领取、LOCK-锁定、UNLOCK-解锁、USE-使用核销、EXPIRE-过期、VOID-作废 */
    private String operationType;

    /** 操作前的优惠券状态 */
    private String beforeStatus;

    /** 操作后的优惠券状态 */
    private String afterStatus;

    /** 业务类型：SALES_ORDER-销售订单、RECHARGE_ORDER-充值订单、ACTIVITY-活动 */
    private String bizType;

    /** 业务流水号，关联具体业务单据 */
    private String bizNo;

    /** 备注说明 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
