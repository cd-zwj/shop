package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值订单实体
 */
@Data
@TableName("recharge_order")
public class RechargeOrder implements Serializable {
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
     * 订单号
     */
    private String orderNo;
    
    /**
     * 充值规则ID
     */
    private Long ruleId;
    
    /**
     * 充值金额
     */
    private BigDecimal rechargeAmount;
    
    /**
     * 赠送金额
     */
    private BigDecimal bonusAmount;
    
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    
    /**
     * 支付状态（0-待支付，1-已支付）
     */
    private Integer payStatus;
    
    /**
     * 支付时间
     */
    private LocalDateTime payTime;
    
    private Integer deleted;
    
    private LocalDateTime createTime;
}
