package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_bill")
public class PaymentBill implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String billNo;
    private String bizType;
    private String bizNo;
    private Long tenantId;
    private Long platformUserId;
    private String channelCode;
    private String channelMode;
    private BigDecimal payAmount;
    private String payStatus;
    private String thirdPartyBillNo;
    private String callbackStatus;
    private String statusRemark;
    private LocalDateTime expireTime;
    private String extensionJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
