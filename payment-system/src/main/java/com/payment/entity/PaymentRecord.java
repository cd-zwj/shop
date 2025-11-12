package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体
 */
@Data
@TableName("payment_record")
public class PaymentRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 租户ID
     */
    private Long tenantId;
    
    private Long orderId;
    
    private String orderNo;
    
    /**
     * 支付方式：WECHAT-微信，ALIPAY-支付宝
     */
    private String payType;
    
    private BigDecimal amount;
    
    private String thirdPartyOrderNo;
    
    private String transactionId;
    
    /**
     * 支付状态：SUCCESS-成功，FAIL-失败，PROCESSING-处理中
     */
    private String payStatus;
    
    private String notifyData;
    
    private LocalDateTime payTime;
    
    private LocalDateTime notifyTime;
    
    private Integer deleted;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}

