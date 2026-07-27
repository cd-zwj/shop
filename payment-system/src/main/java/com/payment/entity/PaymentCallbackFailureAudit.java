package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 被拒绝的支付回调安全审计，不参与支付业务幂等。 */
@Data
@TableName("payment_callback_failure_audit")
public class PaymentCallbackFailureAudit implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventId;
    private String channelCode;
    private String failureReason;
    private String verifyStatus;
    private String candidateBillNo;
    private String providerRequestId;
    private String payloadSha256;
    private Integer payloadSize;
    private Long occurrenceCount;
    private LocalDateTime windowStart;
    private LocalDateTime createTime;
    private LocalDateTime lastTime;
    private LocalDateTime expireTime;
}
