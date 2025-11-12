package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户行为日志实体
 */
@Data
@TableName("user_behavior_log")
public class UserBehaviorLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    private Long userId;
    
    /**
     * 行为类型：LOGIN-登录，PAY-支付，VIEW-浏览，SEARCH-搜索
     */
    private String behaviorType;
    
    private String behaviorData;
    
    private String ipAddress;
    
    private String userAgent;
    
    private LocalDateTime createTime;
}

