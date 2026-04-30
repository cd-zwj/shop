package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("payment_callback_record")
public class PaymentCallbackRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String billNo;
    private String channelCode;
    private String callbackRequestId;
    private String callbackBody;
    private String verifyStatus;
    private String processStatus;
    private LocalDateTime callbackTime;
}
