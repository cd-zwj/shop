package com.payment.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 营销活动视图对象。
 */
@Data
public class PromotionActivityVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String activityNo;
    private Long tenantId;
    private String ownerType;
    private String name;
    private String activityType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
