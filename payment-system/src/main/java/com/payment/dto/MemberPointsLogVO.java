package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员积分变更日志视图对象，用于返回用户积分的收支明细（V1 AppWallet 接口）。
 */
@Data
public class MemberPointsLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志 ID */
    private Long id;
    /** 所属商户租户 ID */
    private Long tenantId;
    /** 用户 ID */
    private Long platformUserId;
    /** 业务类型（如 ORDER_EARN-订单获得、EXCHANGE-积分兑换、REFUND_RETURN-退款归还） */
    private String bizType;
    /** 关联业务单号 */
    private String bizNo;
    /** 变更积分数（正数为获得，负数为消耗） */
    private Integer changePoints;
    /** 变更前积分余额 */
    private Integer pointsBefore;
    /** 变更后积分余额 */
    private Integer pointsAfter;
    /** 备注说明 */
    private String remark;
    /** 状态（如 PENDING-待确认、CONFIRMED-已确认） */
    private String status;
    /** 确认时间 */
    private LocalDateTime confirmTime;
    /** 释放时间（积分冻结后释放） */
    private LocalDateTime releaseTime;
    /** 释放原因 */
    private String releaseReason;
    /** 创建时间 */
    private LocalDateTime createTime;
}
