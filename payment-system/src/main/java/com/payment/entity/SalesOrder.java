package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sales_order")
public class SalesOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long tenantId;
    private Long platformUserId;
    private String orderStatus;
    private String payStatus;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal walletDeductAmount;
    private BigDecimal unifiedWalletDeductAmount;
    private BigDecimal merchantWalletDeductAmount;
    private BigDecimal externalPayAmount;
    private BigDecimal payableAmount;
    private String subject;
    private String source;
    private String walletStrategy;
    private LocalDateTime expireTime;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
