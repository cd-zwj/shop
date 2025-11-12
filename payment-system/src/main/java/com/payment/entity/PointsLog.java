package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分明细实体
 */
@Data
@TableName("points_log")
public class PointsLog implements Serializable {
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
     * 积分变动（正数为增加，负数为扣减）
     */
    private Integer points;
    
    /**
     * 变动后余额
     */
    private Integer balance;
    
    /**
     * 类型（GRANT-发放，DEDUCT-扣减）
     */
    private String type;
    
    /**
     * 原因
     */
    private String reason;
    
    /**
     * 关联订单号
     */
    private String orderNo;
    
    private Integer deleted;
    
    private LocalDateTime createTime;
}
