package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 余额明细实体
 */
@Data
@TableName("balance_log")
public class BalanceLog implements Serializable {
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
     * 金额变动（正数为增加，负数为扣减）
     */
    private BigDecimal amount;
    
    /**
     * 变动后余额
     */
    private BigDecimal balance;
    
    /**
     * 类型（RECHARGE-充值，CONSUME-消费）
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
