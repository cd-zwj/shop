package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 营销活动实体。
 */
@Data
@TableName("promotion_activity")
public class PromotionActivity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
