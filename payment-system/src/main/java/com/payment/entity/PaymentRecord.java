package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体
 * <p>对应数据库表 payment_record，记录每笔支付交易的详细信息和第三方回调数据。
 * 与支付账单（payment_bill）配合使用：payment_bill 记录支付请求的生命周期，
 * payment_record 记录实际的支付结果和第三方通知详情。</p>
 */
@Data
@TableName("payment_record")
public class PaymentRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户 ID，标识所属商户，用于多租户数据隔离
     */
    private Long tenantId;

    /**
     * 关联订单 ID
     */
    private Long orderId;

    /**
     * 关联订单编号，用于业务层关联查询
     */
    private String orderNo;

    /**
     * 支付方式：WECHAT-微信支付，ALIPAY-支付宝
     */
    private String payType;

    /**
     * 支付金额（元）
     */
    private BigDecimal amount;

    /**
     * 第三方支付平台订单号，由支付渠道返回
     */
    private String thirdPartyOrderNo;

    /**
     * 交易流水号，支付平台侧的唯一交易标识
     */
    private String transactionId;

    /**
     * 支付状态：SUCCESS-成功，FAIL-失败，PROCESSING-处理中
     */
    private String payStatus;

    /**
     * 第三方回调原始数据 JSON，保存支付平台异步通知的完整报文，用于对账和问题排查
     */
    private String notifyData;

    /**
     * 支付完成时间，支付成功或失败的时间点
     */
    private LocalDateTime payTime;

    /**
     * 回调通知时间，收到第三方异步通知的时间
     */
    private LocalDateTime notifyTime;

    /**
     * 逻辑删除标记：0-未删除，1-已删除
     */
    private Integer deleted;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;
}
