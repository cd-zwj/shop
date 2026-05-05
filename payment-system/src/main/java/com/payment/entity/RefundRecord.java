package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_record")
public class RefundRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String refundNo;
    private String paymentBillNo;
    private String channelCode;
    private BigDecimal refundAmount;
    private String thirdPartyBillNo;
    private String thirdPartyRefundNo;
    private String channelStatus;
    private String notifyData;
    private LocalDateTime requestTime;
    private LocalDateTime notifyTime;
    private LocalDateTime successTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
