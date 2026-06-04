package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户积分实体
 */
@Data
@TableName("user_points")
public class UserPoints implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 积分余额
     */
    private Integer points;
    
    /**
     * 累计获得
     */
    private Integer totalEarned;
    
    /**
     * 累计使用
     */
    private Integer totalUsed;
    
    private Integer deleted;

    @Version
    private Integer version;

    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
