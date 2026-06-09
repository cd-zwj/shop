package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员成长值日志实体。
 */
@Data
@TableName("member_growth_log")
public class MemberGrowthLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long platformUserId;
    /** 变动类型：EARN-增加，DEDUCT-扣减，ADJUST-调整 */
    private String changeType;
    private Integer changeGrowth;
    private Integer growthBefore;
    private Integer growthAfter;
    private Long levelId;
    /** 业务类型：ORDER-订单，RECHARGE-充值，MANUAL-人工 */
    private String bizType;
    private String bizNo;
    private String remark;
    private LocalDateTime createTime;
}
