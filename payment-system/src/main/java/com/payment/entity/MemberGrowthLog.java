package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员成长值日志实体，对应数据库表 member_growth_log。
 * 记录会员成长值的每一次变动明细，用于追踪等级升降的来源。
 * 成长值不同于积分，通常不可消费，仅用于会员等级的判定。
 */
@Data
@TableName("member_growth_log")
public class MemberGrowthLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID，多租户隔离标识 */
    private Long tenantId;

    /** 平台用户ID，关联 platform_user 表 */
    private Long platformUserId;

    /** 变动类型：EARN-增加，DEDUCT-扣减，ADJUST-调整 */
    private String changeType;

    /** 本次变动的成长值数量，正数为增加，负数为扣减 */
    private Integer changeGrowth;

    /** 变动前成长值 */
    private Integer growthBefore;

    /** 变动后成长值 */
    private Integer growthAfter;

    /** 关联的会员等级ID，记录变动时对应的等级 */
    private Long levelId;

    /** 业务类型：ORDER-订单，RECHARGE-充值，MANUAL-人工 */
    private String bizType;

    /** 业务单号，关联具体的业务流水 */
    private String bizNo;

    /** 备注说明 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
