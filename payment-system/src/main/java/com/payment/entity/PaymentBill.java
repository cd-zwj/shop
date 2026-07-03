package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付账单实体
 * <p>对应数据库表 payment_bill，记录每一笔支付请求的全生命周期信息。
 * 包含支付渠道、金额、状态、第三方回调等数据，是支付流程的核心账单载体。
 * 一次业务订单可能对应一笔或多笔支付账单（如部分退款后的补单）。</p>
 */
@Data
@TableName("payment_bill")
public class PaymentBill implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账单编号，系统生成的唯一业务编号，用于外部关联和查询
     */
    private String billNo;

    /**
     * 业务类型，标识账单关联的业务场景，如 ORDER-订单支付、RECHARGE-充值、WITHDRAW-提现
     */
    private String bizType;

    /**
     * 业务单号，关联具体的业务订单编号
     */
    private String bizNo;

    /**
     * 租户 ID，标识所属商户，用于多租户数据隔离
     */
    private Long tenantId;

    /**
     * 平台用户 ID，发起支付的用户
     */
    private Long platformUserId;

    /**
     * 支付渠道编码，如 WECHAT-微信支付、ALIPAY-支付宝、WALLET-钱包支付
     */
    private String channelCode;

    /**
     * 支付渠道模式，如 JSAPI、NATIVE、H5、APP 等
     */
    private String channelMode;

    /**
     * 支付金额（元）
     */
    private BigDecimal payAmount;

    /**
     * 支付状态：WAIT_PAY-待支付、PAYING-支付中、SUCCESS-支付成功、FAIL-支付失败、CLOSED-已关闭
     */
    private String payStatus;

    /**
     * 第三方支付平台流水号，支付成功后由微信/支付宝等返回
     */
    private String thirdPartyBillNo;

    /**
     * 回调状态：NONE-未回调、SUCCESS-回调成功、FAIL-回调失败，用于记录第三方异步通知处理结果
     */
    private String callbackStatus;

    /**
     * 状态备注，记录支付失败原因或状态变更说明
     */
    private String statusRemark;

    /**
     * 账单过期时间，超过此时间未支付则自动关闭
     */
    private LocalDateTime expireTime;

    /**
     * 扩展信息 JSON，存储与支付渠道相关的附加参数（如微信 openid、支付宝买家号等）
     */
    private String extensionJson;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;
}
