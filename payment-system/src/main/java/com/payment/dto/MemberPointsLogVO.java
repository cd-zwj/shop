package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员积分变更日志视图对象（V1 AppWallet 接口）
 */
@Data
public class MemberPointsLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long platformUserId;
    private String bizType;
    private String bizNo;
    private Integer changePoints;
    private Integer pointsBefore;
    private Integer pointsAfter;
    private String remark;
    private String status;
    private LocalDateTime confirmTime;
    private LocalDateTime releaseTime;
    private String releaseReason;
    private LocalDateTime createTime;
}
