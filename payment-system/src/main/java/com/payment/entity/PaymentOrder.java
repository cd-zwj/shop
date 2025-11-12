package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("payment_order")
public class PaymentOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 订单金额
     */
    private BigDecimal amount;
    
    /**
     * 实付金额
     */
    private BigDecimal payAmount;
    
    /**
     * 支付方式：WECHAT-微信，ALIPAY-支付宝
     */
    private String payType;
    
    /**
     * 订单状态：PENDING-待支付，PAID-已支付，CANCELLED-已取消，REFUNDED-已退款
     */
    private String orderStatus;
    
    /**
     * 支付状态：SUCCESS-成功，FAIL-失败
     */
    private String payStatus;
    
    /**
     * 第三方订单号
     */
    private String thirdPartyOrderNo;
    
    /**
     * 订单标题
     */
    private String subject;
    
    /**
     * 订单描述
     */
    private String body;
    
    /**
     * 支付时间
     */
    private LocalDateTime payTime;
    
    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;
    
    /**
     * 回调地址
     */
    private String notifyUrl;
    
    private Integer deleted;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}

