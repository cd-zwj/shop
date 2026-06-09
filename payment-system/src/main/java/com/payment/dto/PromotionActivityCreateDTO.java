package com.payment.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 营销活动创建参数。
 */
@Data
public class PromotionActivityCreateDTO {
    private Long tenantId;
    private String activityScope;
    private String activityName;
    private String activityType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String description;
}
