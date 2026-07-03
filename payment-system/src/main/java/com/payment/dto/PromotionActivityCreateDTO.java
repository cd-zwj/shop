package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 营销活动创建请求参数。
 */
@Data
public class PromotionActivityCreateDTO {

    /** 所属租户 ID */
    private Long tenantId;

    /** 活动范围（PLATFORM-平台级 / MERCHANT-商户级） */
    private String activityScope;

    /** 活动名称 */
    private String activityName;

    /** 活动类型（如：DISCOUNT-折扣 / FULL_REDUCTION-满减） */
    private String activityType;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 活动描述 */
    private String description;
}
