package com.payment.service;

import com.payment.dto.PayResponseDTO;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.entity.PaymentBill;
import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.enums.PaymentStatusReasonEnum;

import java.math.BigDecimal;
import java.util.List;

/**
 * 支付账单服务接口（V1 版本）。
 * <p>
 * 负责支付账单的全生命周期管理，包括账单创建、外部支付发起、
 * 支付回调处理、账单关闭及状态同步。
 * 每一笔业务订单（如商品订单、充值订单）可关联一张或多张支付账单。
 */
public interface PaymentBillV1Service {

    /**
     * 创建支付账单。
     * <p>
     * 为指定业务单据生成支付账单，记录应付金额和支付渠道信息。
     *
     * @param bizType        业务类型（如 ORDER、RECHARGE 等）
     * @param bizNo          业务单号
     * @param tenantId       租户 ID
     * @param platformUserId 平台用户 ID
     * @param payAmount      应付金额（单位：元）
     * @param channelCode    支付渠道枚举（微信、支付宝等）
     * @return 创建的支付账单实体
     */
    PaymentBill createBill(String bizType,
                           String bizNo,
                           Long tenantId,
                           Long platformUserId,
                           BigDecimal payAmount,
                           PaymentChannelCodeEnum channelCode);

    /**
     * 发起外部支付（调用微信/支付宝等第三方支付渠道）。
     *
     * @param paymentBill 支付账单实体
     * @return 支付响应 DTO，包含预支付参数（如微信 JSAPI 参数）或收银台 URL
     */
    PayResponseDTO createExternalPayment(PaymentBill paymentBill);

    /**
     * 处理支付回调通知。
     * <p>
     * 接收第三方支付平台的异步通知，更新账单状态并触发后续业务流程（如订单状态变更）。
     *
     * @param channelCode  支付渠道代码
     * @param callbackDTO  回调数据
     */
    void handleCallback(String channelCode, PaymentCallbackDTO callbackDTO);

    /**
     * 根据业务单号关闭关联的支付账单。
     * <p>
     * 当业务订单关闭/取消时调用，将对应的未支付账单标记为已关闭。
     *
     * @param bizType      业务类型
     * @param bizNo        业务单号
     * @param statusReason 关闭原因枚举
     */
    void markBizClosed(String bizType, String bizNo, PaymentStatusReasonEnum statusReason);

    /**
     * 根据账单号关闭指定支付账单。
     *
     * @param billNo       支付账单号
     * @param statusReason 关闭原因枚举
     */
    void markBillClosed(String billNo, PaymentStatusReasonEnum statusReason);

    /**
     * 根据账单号查询支付账单。
     *
     * @param billNo 支付账单号
     * @return 支付账单实体，不存在时返回 null
     */
    PaymentBill getByBillNo(String billNo);

    /**
     * 查询指定业务单据最近的一张支付账单。
     *
     * @param bizType 业务类型
     * @param bizNo   业务单号
     * @return 最近创建的支付账单，不存在时返回 null
     */
    PaymentBill getLatestByBizTypeAndBizNo(String bizType, String bizNo);

    /**
     * 查询指定业务单据关联的所有支付账单。
     *
     * @param bizType 业务类型
     * @param bizNo   业务单号
     * @return 支付账单列表
     */
    List<PaymentBill> listByBizTypeAndBizNo(String bizType, String bizNo);

    /**
     * 主动同步账单状态。
     * <p>
     * 向第三方支付平台查询最新支付结果，用于处理回调丢失或超时的场景。
     *
     * @param billNo 支付账单号
     * @return 状态同步后的支付账单实体
     */
    PaymentBill syncBillStatus(String billNo);
}
