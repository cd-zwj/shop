package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("refund_reconcile_task")
public class RefundReconcileTask implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private String refundNo;
    private String channelCode;
    private String taskStatus;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime nextRetryTime;
    private String lastResult;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
