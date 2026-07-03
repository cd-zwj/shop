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
 * 支持多种业务场景（支付回调、订单关闭、退款查询等）的失败重试，
 * 采用指数退避策略，超过最大重试次数后标记为 DEAD 进入死信处理。
 */
@Data
@TableName("retry_task")
public class RetryTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
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

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
