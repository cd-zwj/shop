package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 补偿任务实体。
 * 对应 compensation_task 表，用于记录需要补偿执行的业务操作。
 * 当因系统异常（如 MQ 消费失败、支付回调丢失等）导致业务流程中断时，
 * 创建补偿任务由定时任务扫描执行，保障业务最终一致性。
 */
@Data
@TableName("compensation_task")
public class CompensationTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 补偿任务编号（唯一） */
    private String taskNo;

    /** 业务类型，如 ORDER_CLOSE、PAYMENT_NOTIFY、REFUND_PROCESS 等 */
    private String bizType;

    /** 关联的业务单号 */
    private String bizNo;

    /** 任务状态：PENDING-待处理、PROCESSING-处理中、SUCCESS-成功、FAIL-失败、CANCELLED-已取消 */
    private String taskStatus;

    /** 备注说明 */
    private String remark;

    /** 已重试次数 */
    private Integer retryCount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
