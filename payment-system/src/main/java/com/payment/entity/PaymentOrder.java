package com.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单实体，对应数据库表 payment_order。
 *
 * @deprecated 使用 SalesOrder + PaymentBill 替代，该实体为旧版订单模型，保留仅供兼容。
 * <p>
 * 记录用户发起的支付请求及支付结果，包含订单金额、支付方式、第三方交易号等信息。
 * </p>
 */
@Data
@TableName("payment_order")
@Deprecated
public class PaymentOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 支付方式：WECHAT-微信，ALIPAY-支付宝
     */
    private String payType;

    /**
     * 订单状态：PENDING-待支付，PAID-已支付，CANCELLED-已取消，REFUNDED-已退款
     */
    private String orderStatus;

    /**
     * 支付状态：SUCCESS-成功，FAIL-失败
     */
    private String payStatus;

    /**
     * 第三方订单号
     */
    private String thirdPartyOrderNo;

    /**
     * 订单标题
     */
    private String subject;

    /**
     * 订单描述
     */
    private String body;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 订单过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 回调地址
     */
    private String notifyUrl;

    /**
     * 交易类型：NATIVE-Native支付，JSAPI-小程序/公众号支付
     */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String tradeType;

    /** 逻辑删除标记，0-未删除，1-已删除 */
    private Integer deleted;

    /** 订单创建时间 */
    private LocalDateTime createTime;

    /** 订单最后更新时间 */
    private LocalDateTime updateTime;
}
