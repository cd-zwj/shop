package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 营销活动视图对象，展示活动的完整信息。
 */
@Data
public class PromotionActivityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动 ID */
    private Long id;

    /** 活动编号 */
    private String activityNo;

    /** 所属租户 ID */
    private Long tenantId;

    /** 活动范围（PLATFORM / MERCHANT） */
    private String activityScope;

    /** 活动名称 */
    private String activityName;

    /** 活动类型 */
    private String activityType;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 活动状态（DRAFT / ACTIVE / EXPIRED / DISABLED） */
    private String status;

    /** 活动描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
