package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_order")
public class RefundOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String refundNo;
    private String bizType;
    private String bizNo;
    private Long tenantId;
    private Long platformUserId;
    private String orderNo;
    private String paymentBillNo;
    private String channelCode;
    private String refundReason;
    private BigDecimal applyAmount;
    private BigDecimal refundAmount;
    private BigDecimal walletRefundAmount;
    private BigDecimal externalRefundAmount;
    private String refundStatus;
    private String auditStatus;
    private Long auditBy;
    private LocalDateTime auditTime;
    private LocalDateTime successTime;
    private String failReason;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
