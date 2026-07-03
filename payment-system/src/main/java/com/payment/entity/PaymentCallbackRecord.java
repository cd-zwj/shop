package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付回调记录，对应数据库表 payment_callback_record。
 * <p>
 * 记录第三方支付平台（微信/支付宝）的每笔回调通知，用于幂等保障和问题排查。
 * 通过 callbackRequestId 实现幂等，同一回调请求不会重复处理。
 * </p>
 */
@Data
@TableName("payment_callback_record")
public class PaymentCallbackRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的支付账单编号，关联 payment_bill.bill_no */
    private String billNo;

    /** 支付渠道编码，如：WECHAT_PAY / ALIPAY */
    private String channelCode;

    /** 回调请求唯一标识，用于幂等校验，防止同一回调重复处理 */
    private String callbackRequestId;

    /** 回调原始报文 JSON，完整保存第三方推送的请求体，便于问题排查 */
    private String callbackBody;

    /**
     * 签名验证状态，取值如：SUCCESS(验证通过) / FAIL(验证失败)
     */
    private String verifyStatus;

    /**
     * 回调处理状态，取值如：PENDING(待处理) / SUCCESS(处理成功) / FAIL(处理失败)
     */
    private String processStatus;

    /** 收到回调的时间 */
    private LocalDateTime callbackTime;
}
