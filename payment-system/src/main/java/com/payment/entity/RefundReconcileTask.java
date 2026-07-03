package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退款对账任务实体。
 * 对应数据库表 refund_reconcile_task，用于退款流程中的渠道对账与补偿。
 * <p>当退款请求发送到第三方支付渠道后，若回调长时间未到达或状态不一致，
 * 系统通过本表记录对账任务，定时轮询渠道查询退款状态，确保退款最终一致性。
 * <p>支持重试机制：按 retryCount / maxRetryCount 控制重试次数，nextRetryTime 控制重试间隔。
 */
@Data
@TableName("refund_reconcile_task")
public class RefundReconcileTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对账任务编号，全局唯一 */
    private String taskNo;

    /** 关联的退款单号，对应 RefundOrder 或 RefundRecord */
    private String refundNo;

    /** 支付渠道编码（如 WECHAT_PAY、ALIPAY 等） */
    private String channelCode;

    /**
     * 对账任务状态。
     * 如：PENDING=待执行，PROCESSING=执行中，SUCCESS=对账成功，FAILED=对账失败
     */
    private String taskStatus;

    /** 当前已重试次数 */
    private Integer retryCount;

    /** 最大重试次数，超过后标记为失败 */
    private Integer maxRetryCount;

    /** 下次重试时间 */
    private LocalDateTime nextRetryTime;

    /** 最近一次对账查询的结果（JSON），记录渠道返回的状态信息 */
    private String lastResult;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
}
