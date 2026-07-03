package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员积分日志实体，对应数据库表 member_points_log。
 * 记录会员积分的每一笔变动明细，包括获得、消耗、过期等场景，
 * 支持待确认和待释放等中间状态，适用于积分到账前的审核流程。
 */
@Data
@TableName("member_points_log")
public class MemberPointsLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID，多租户隔离标识 */
    private Long tenantId;

    /** 平台用户ID，关联 platform_user 表 */
    private Long platformUserId;

    /** 业务类型：ORDER-订单，RECHARGE-充值，SIGNIN-签到，REFUND-退款，MANUAL-人工调整等 */
    private String bizType;

    /** 业务单号，关联具体的业务流水 */
    private String bizNo;

    /** 本次变动的积分数量，正数为获得，负数为消耗 */
    private Integer changePoints;

    /** 变动前积分余额 */
    private Integer pointsBefore;

    /** 变动后积分余额 */
    private Integer pointsAfter;

    /** 备注说明 */
    private String remark;

    /** 日志状态：PENDING-待确认，CONFIRMED-已确认，RELEASED-已释放，CANCELLED-已取消 */
    private String status;

    /** 积分过期时间，用于设置积分有效期 */
    private LocalDateTime expireTime;

    /** 确认时间，积分由待确认变为已确认的时间点 */
    private LocalDateTime confirmTime;

    /** 释放时间，积分由待释放变为实际到账的时间点 */
    private LocalDateTime releaseTime;

    /** 释放原因，记录积分释放或取消的说明 */
    private String releaseReason;

    /** 创建时间 */
    private LocalDateTime createTime;
}
