package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 重试任务实体。
 * 对应 retry_task 表，用于统一管理各类异步任务的重试调度。
 */
@Data
@TableName("retry_task")
public class RetryTask implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编号（唯一） */
    private String taskNo;

    /** 任务类型：PAYMENT_CALLBACK / ORDER_CLOSE / RECHARGE_CREDIT / REFUND_QUERY / COUPON_COMPENSATE / SMS_RETRY */
    private String taskType;

    /** 业务类型 */
    private String bizType;

    /** 业务单号 */
    private String bizNo;

    /** 关联消息ID */
    private String messageId;

    /** 状态：PENDING / PROCESSING / SUCCESS / FAIL / DEAD / CANCELLED */
    private String taskStatus;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数（默认 16） */
    private Integer maxRetryCount;

    /** 下次重试时间 */
    private LocalDateTime nextRetryTime;

    /** 最后一次错误信息 */
    private String lastErrorMessage;

    /** 扩展信息（JSON） */
    private String extensionJson;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
