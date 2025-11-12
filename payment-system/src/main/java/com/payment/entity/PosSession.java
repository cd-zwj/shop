package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收银会话实体
 */
@Data
@TableName("pos_session")
public class PosSession implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 设备ID
     */
    private String deviceId;
    
    /**
     * 状态（0-进行中，1-已结账，2-已取消）
     */
    private Integer status;
    
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    
    private Integer deleted;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
