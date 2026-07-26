package com.payment.service.impl;

import com.payment.util.JsonUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.constant.RefundConstants;
import com.payment.common.BusinessException;
import com.payment.config.RabbitMQConfig;
import com.payment.dto.ExternalPaymentQueryResult;
import com.payment.dto.PayResponseDTO;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.entity.MessageOutbox;
import com.payment.entity.PaymentBill;
import com.payment.entity.PaymentCallbackRecord;
import com.payment.entity.SalesOrder;
import com.payment.enums.CallbackStatusEnum;
import com.payment.enums.MessageProcessStatusEnum;
import com.payment.enums.OrderStatusEnum;
import com.payment.enums.PayStatusEnum;
import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.enums.PaymentLateCallbackActionEnum;
import com.payment.enums.PaymentStatusReasonEnum;
import com.payment.enums.PaymentBizTypeEnum;
import com.payment.mapper.PaymentBillMapper;
import com.payment.mapper.PaymentCallbackRecordMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.CompensationTaskFactory;
import com.payment.service.OutboxPublisher;
import com.payment.service.PaymentBillV1Service;
import com.payment.service.PaymentProvider;
import com.payment.service.RefundService;
import com.payment.service.outbox.OutboxMessageCommand;
import com.payment.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 支付账单核心服务实现。
 * <p>
 * 负责支付单的全生命周期管理，包括：
 * <ul>
 *   <li>创建支付账单并发起第三方支付</li>
 *   <li>处理支付异步回调（验签、幂等、状态流转）</li>
 *   <li>标记业务关闭（超时、取消等场景）</li>
 *   <li>主动同步第三方支付状态</li>
 *   <li>支付成功后通过 Outbox 发布业务事件</li>
 * </ul>
 * 迟到回调（Late Callback）支持三种处理策略：直接标记成功、触发退款、转人工审核。
 * 所有状态变更均通过事务保护。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentBillV1ServiceImpl implements PaymentBillV1Service {

    private final PaymentBillMapper paymentBillMapper;
    private final PaymentCallbackRecordMapper callbackRecordMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final CompensationTaskFactory compensationTaskFactory;
    private final OutboxPublisher outboxPublisher;
    private final List<PaymentProvider> paymentProviders;
    private final RefundService refundService;

    /**
     * 创建支付账单。
     * <p>
     * 生成唯一账单号（PB 前缀），设置初始状态为待支付，有效期 30 分钟。
     *
     * @param bizType       业务类型（如 ORDER、RECHARGE）
     * @param bizNo         业务单号
     * @param tenantId      租户 ID（可为 null）
     * @param platformUserId 平台用户 ID
     * @param payAmount     支付金额
     * @param channelCode   支付渠道编码
     * @return 新创建的支付账单实体
     * @throws BusinessException 支付渠道为空时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentBill createBill(String bizType,
                                  String bizNo,
                                  Long tenantId,
                                  Long platformUserId,
                                  BigDecimal payAmount,
                                  PaymentChannelCodeEnum channelCode) {
        if (channelCode == null) {
            throw new BusinessException("支付渠道不能为空");
        }
        PaymentBill paymentBill = new PaymentBill();
        paymentBill.setBillNo(BizNoGenerator.generate("PB"));
        paymentBill.setBizType(bizType);
        paymentBill.setBizNo(bizNo);
        paymentBill.setTenantId(tenantId);
        paymentBill.setPlatformUserId(platformUserId);
        paymentBill.setChannelCode(channelCode.name());
        paymentBill.setChannelMode("REDIRECT");
        paymentBill.setPayAmount(payAmount);
        paymentBill.setPayStatus(PayStatusEnum.WAIT_PAY.name());
        paymentBill.setCallbackStatus(CallbackStatusEnum.NOT_CALLBACK.name());
        paymentBill.setStatusRemark(null);
        paymentBill.setExpireTime(LocalDateTime.now().plusMinutes(30));
        paymentBillMapper.insert(paymentBill);
        return paymentBill;
    }

    /**
     * 调用第三方支付渠道创建实际支付。
     *
     * @param paymentBill 已持久化的支付账单
     * @return 包含支付链接的响应 DTO
     */
    @Override
    public PayResponseDTO createExternalPayment(PaymentBill paymentBill) {
        return getProvider(paymentBill.getChannelCode()).createPayment(paymentBill);
    }

    /**
     * 处理第三方支付回调。
     * <p>
     * 流程：幂等检查（按回调请求 ID 去重） → 验签 → 状态更新 → 发布业务事件。
     * 已成功的账单不会重复处理。
     *
     * @param channelCode 支付渠道编码
     * @param callbackDTO 回调数据
     * @throws BusinessException 账单不存在或验签失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleCallback(String channelCode, PaymentCallbackDTO callbackDTO) {
        PaymentBill paymentBill = getByBillNo(callbackDTO.getBillNo());
        if (paymentBill == null) {
            throw new BusinessException("支付单不存在");
        }

        PaymentProvider provider = getProvider(channelCode);
        PaymentCallbackRecord existingRecord = callbackRecordMapper.selectOne(new LambdaQueryWrapper<PaymentCallbackRecord>()
                .eq(PaymentCallbackRecord::getChannelCode, channelCode)
                .eq(PaymentCallbackRecord::getCallbackRequestId, callbackDTO.getCallbackRequestId()));
        if (existingRecord != null) {
            return;
        }

        boolean verified = provider.verifyCallback(callbackDTO);

        PaymentCallbackRecord callbackRecord = new PaymentCallbackRecord();
        callbackRecord.setBillNo(paymentBill.getBillNo());
        callbackRecord.setChannelCode(channelCode);
        callbackRecord.setCallbackRequestId(callbackDTO.getCallbackRequestId());
        callbackRecord.setCallbackBody(callbackDTO.getRawBody());
        callbackRecord.setVerifyStatus(verified ? MessageProcessStatusEnum.SUCCESS.name() : MessageProcessStatusEnum.FAILED.name());
        callbackRecord.setProcessStatus(MessageProcessStatusEnum.PENDING.name());
        callbackRecordMapper.insert(callbackRecord);

        if (!verified) {
            callbackRecord.setProcessStatus(MessageProcessStatusEnum.FAILED.name());
            callbackRecordMapper.updateById(callbackRecord);
            throw new BusinessException("支付回调验签失败");
        }

        if (PayStatusEnum.SUCCESS.name().equals(paymentBill.getPayStatus())) {
            callbackRecord.setProcessStatus(MessageProcessStatusEnum.SUCCESS.name());
            callbackRecordMapper.updateById(callbackRecord);
            return;
        }

        handlePaidResult(paymentBill, callbackDTO.getThirdPartyBillNo(), CallbackStatusEnum.CALLBACK_SUCCESS.name());
        callbackRecord.setProcessStatus(MessageProcessStatusEnum.SUCCESS.name());
        callbackRecordMapper.updateById(callbackRecord);
    }

    /**
     * 根据业务单号关闭支付账单（如订单取消、超时等场景）。
     *
     * @param bizType      业务类型
     * @param bizNo        业务单号
     * @param statusReason 关闭原因枚举
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markBizClosed(String bizType, String bizNo, PaymentStatusReasonEnum statusReason) {
        if (statusReason == null) {
            throw new BusinessException("支付关闭原因不能为空");
        }

        PaymentBill paymentBill = paymentBillMapper.selectOne(new LambdaQueryWrapper<PaymentBill>()
                .eq(PaymentBill::getBizType, bizType)
                .eq(PaymentBill::getBizNo, bizNo));
        if (paymentBill == null) {
            return;
        }
        closePaymentBill(paymentBill, statusReason);
    }

    /**
     * 根据账单号关闭支付账单。
     *
     * @param billNo       支付账单号
     * @param statusReason 关闭原因枚举
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markBillClosed(String billNo, PaymentStatusReasonEnum statusReason) {
        if (statusReason == null) {
            throw new BusinessException("支付关闭原因不能为空");
        }

        PaymentBill paymentBill = getByBillNo(billNo);
        if (paymentBill == null) {
            return;
        }
        closePaymentBill(paymentBill, statusReason);
    }

    /**
     * 关闭支付账单（内部方法）。
     * <p>
     * 条件更新闭账：仅 WAIT_PAY/PAYING -> CLOSED，记录关闭原因到扩展 JSON。
     * 并发场景下若账单已被渠道确认成功，本次关闭不生效（受影响行数 0），
     * 调用方据此可知账单已进入迟到回调退款流程。重读真实状态回填入参实体，
     * 便于上层据真实状态决定后续动作。
     */
    private void closePaymentBill(PaymentBill paymentBill, PaymentStatusReasonEnum statusReason) {
        if (PayStatusEnum.SUCCESS.name().equals(paymentBill.getPayStatus())) {
            return;
        }
        String extensionJson = buildClosedStatusReasonJson(paymentBill, statusReason);
        int updated = paymentBillMapper.closeIfPending(
                paymentBill.getBillNo(), statusReason.getRemark(), extensionJson);
        if (updated == 0) {
            PaymentBill latest = paymentBillMapper.selectOne(new LambdaQueryWrapper<PaymentBill>()
                    .eq(PaymentBill::getBillNo, paymentBill.getBillNo()));
            if (latest != null) {
                paymentBill.setPayStatus(latest.getPayStatus());
                paymentBill.setStatusRemark(latest.getStatusRemark());
                paymentBill.setExtensionJson(latest.getExtensionJson());
            }
            return;
        }
        paymentBill.setPayStatus(PayStatusEnum.CLOSED.name());
        paymentBill.setStatusRemark(statusReason.getRemark());
        paymentBill.setExtensionJson(extensionJson);
    }

    /**
     * 构造关闭原因写入的扩展 JSON（原 applyStatusReason 的非破坏性版本，
     * 避免在条件 UPDATE 前先污染内存实体字段影响判断）。
     */
    private String buildClosedStatusReasonJson(PaymentBill paymentBill, PaymentStatusReasonEnum statusReason) {
        ObjectNode extension = parseExtensionJson(paymentBill.getExtensionJson());
        extension.put(RefundConstants.EXTENSION_STATUS_REASON_CODE, statusReason.getCode());
        return JsonUtils.toJson(extension);
    }

    /**
     * 根据账单号查询支付账单。
     */
    @Override
    public PaymentBill getByBillNo(String billNo) {
        return paymentBillMapper.selectOne(new LambdaQueryWrapper<PaymentBill>()
                .eq(PaymentBill::getBillNo, billNo));
    }

    /**
     * 获取指定业务单号对应的最新一笔支付账单。
     *
     * @param bizType 业务类型
     * @param bizNo   业务单号
     * @return 最新支付账单，不存在时返回 null
     */
    @Override
    public PaymentBill getLatestByBizTypeAndBizNo(String bizType, String bizNo) {
        List<PaymentBill> paymentBills = listByBizTypeAndBizNo(bizType, bizNo);
        return paymentBills.isEmpty() ? null : paymentBills.get(0);
    }

    /**
     * 列出指定业务单号对应的所有支付账单，按创建时间降序排列。
     *
     * @param bizType 业务类型
     * @param bizNo   业务单号
     * @return 支付账单列表
     */
    @Override
    public List<PaymentBill> listByBizTypeAndBizNo(String bizType, String bizNo) {
        return paymentBillMapper.selectList(new LambdaQueryWrapper<PaymentBill>()
                .eq(PaymentBill::getBizType, bizType)
                .eq(PaymentBill::getBizNo, bizNo)
                .orderByDesc(PaymentBill::getCreateTime)
                .orderByDesc(PaymentBill::getId));
    }

    /**
     * 主动向第三方支付渠道同步账单状态。
     * <p>
     * 若查询结果为已支付，则更新账单状态并发布业务成功事件。
     * 已成功的账单直接返回，不重复查询。
     *
     * @param billNo 支付账单号
     * @return 最新的支付账单实体
     * @throws BusinessException 账单不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentBill syncBillStatus(String billNo) {
        PaymentBill paymentBill = getByBillNo(billNo);
        if (paymentBill == null) {
            throw new BusinessException("支付单不存在");
        }
        if (PayStatusEnum.SUCCESS.name().equals(paymentBill.getPayStatus())) {
            return paymentBill;
        }

        ExternalPaymentQueryResult queryResult = getProvider(paymentBill.getChannelCode()).queryPayment(paymentBill);
        if (!queryResult.isSuccess()) {
            return paymentBill;
        }

        if (queryResult.isPaid()) {
            handlePaidResult(paymentBill, firstNonBlank(queryResult.getProviderTradeNo(), queryResult.getChannelTradeNo()),
                    CallbackStatusEnum.CALLBACK_SUCCESS.name());
            return getByBillNo(billNo);
        }

        return paymentBill;
    }

    /**
     * 处理支付成功结果。
     * <p>
     * 销售订单以 sales_order 行锁作为支付成功与超时关闭的唯一裁决点：
     * CREATED/WAIT_PAY 抢占为 PAID/SUCCESS 后才允许发布 ORDER_PAID；若订单已关闭，
     * 只登记渠道资金成功并触发迟到退款，绝不恢复订单或发布履约事件。
     */
    private void handlePaidResult(PaymentBill paymentBill, String thirdPartyBillNo, String callbackStatus) {
        if (PayStatusEnum.SUCCESS.name().equals(paymentBill.getPayStatus())) {
            return;
        }

        if (PaymentBizTypeEnum.SALES_ORDER.name().equals(paymentBill.getBizType())) {
            SalesOrder order = salesOrderMapper.selectByOrderNoForUpdate(paymentBill.getBizNo());
            if (order == null) {
                throw new BusinessException("支付单关联订单不存在: " + paymentBill.getBizNo());
            }
            // 获取订单锁后重读账单，避免使用等待锁期间已经被关闭的陈旧状态。
            PaymentBill latestBill = getByBillNo(paymentBill.getBillNo());
            if (latestBill != null) {
                paymentBill = latestBill;
            }
            if (PayStatusEnum.SUCCESS.name().equals(paymentBill.getPayStatus())) {
                return;
            }
            if (PayStatusEnum.CLOSED.name().equals(paymentBill.getPayStatus())) {
                PaymentStatusReasonEnum reason = resolveStatusReason(paymentBill);
                if (reason == null) {
                    reason = PaymentStatusReasonEnum.MANUAL_REVIEW_REQUIRED;
                }
                handleClosedBillLateSuccess(paymentBill, thirdPartyBillNo, callbackStatus, reason);
                return;
            }

            if (OrderStatusEnum.CLOSED.name().equals(order.getOrderStatus())
                    || OrderStatusEnum.CANCELLED.name().equals(order.getOrderStatus())) {
                PaymentStatusReasonEnum reason = resolveStatusReason(paymentBill);
                if (reason == null) {
                    reason = OrderStatusEnum.CLOSED.name().equals(order.getOrderStatus())
                            ? PaymentStatusReasonEnum.SALES_ORDER_TIMEOUT_REFUND_REQUIRED
                            : PaymentStatusReasonEnum.SALES_ORDER_CANCELLED_REFUND_REQUIRED;
                }
                handleClosedBillLateSuccess(paymentBill, thirdPartyBillNo, callbackStatus, reason);
                return;
            }

            if (OrderStatusEnum.CREATED.name().equals(order.getOrderStatus())
                    && PayStatusEnum.WAIT_PAY.name().equals(order.getPayStatus())) {
                int claimed = salesOrderMapper.claimPayment(order.getId());
                if (claimed != 1) {
                    throw new BusinessException("订单支付状态并发更新，请重试: " + order.getOrderNo());
                }
                // 在同一事务中：订单先取得支付所有权，再将账单置成功并写 Outbox。
                markBillPaid(paymentBill, thirdPartyBillNo, callbackStatus, true, null, true);
                return;
            }

            // PAID 说明另一支付成功事务已抢占；PENDING_PREPARATION 及后续表示已处理。
            // 都不得重复发布 Outbox。若账单仍未成功，仅补记资金状态。
            if (PayStatusEnum.SUCCESS.name().equals(order.getPayStatus())) {
                markBillPaid(paymentBill, thirdPartyBillNo, callbackStatus, false, null, true);
                return;
            }
            throw new BusinessException("当前订单状态不允许确认支付: " + order.getOrderStatus());
        }

        PaymentStatusReasonEnum statusReason = resolveStatusReason(paymentBill);
        if (PayStatusEnum.CLOSED.name().equals(paymentBill.getPayStatus())) {
            handleClosedBillLateSuccess(paymentBill, thirdPartyBillNo, callbackStatus, statusReason);
            return;
        }
        markBillPaid(paymentBill, thirdPartyBillNo, callbackStatus, true, null, true);
    }

    /**
     * 处理已关闭账单的迟到回调。
     * <p>
     * 根据关闭原因的迟到回调策略执行不同操作：
     * <ul>
     *   <li>MARK_SUCCESS：直接标记为支付成功</li>
     *   <li>TRIGGER_REFUND：标记成功并触发退款流程</li>
     *   <li>其他：标记成功但不发布业务事件，创建人工审核补偿任务</li>
     * </ul>
     */
    private void handleClosedBillLateSuccess(PaymentBill paymentBill,
                                             String thirdPartyBillNo,
                                             String callbackStatus,
                                             PaymentStatusReasonEnum statusReason) {
        PaymentStatusReasonEnum effectiveReason = statusReason == null
                ? PaymentStatusReasonEnum.MANUAL_REVIEW_REQUIRED
                : statusReason;

        if (PaymentLateCallbackActionEnum.MARK_SUCCESS.equals(effectiveReason.getLateCallbackAction())) {
            markBillPaid(
                    paymentBill,
                    thirdPartyBillNo,
                    callbackStatus,
                    true,
                    "Recovered by late callback: " + effectiveReason.getRemark(),
                    true
            );
            return;
        }

        if (PaymentLateCallbackActionEnum.TRIGGER_REFUND.equals(effectiveReason.getLateCallbackAction())) {
            markBillPaid(
                    paymentBill,
                    thirdPartyBillNo,
                    callbackStatus,
                    false,
                    "Late callback received after business close. Refund is required. Original reason: " + effectiveReason.getRemark(),
                    false
            );
            refundService.prepareLateCallbackRefund(paymentBill, effectiveReason);
            return;
        }

        markBillPaid(
                paymentBill,
                thirdPartyBillNo,
                callbackStatus,
                false,
                "Late callback requires manual review. Original reason: " + effectiveReason.getRemark(),
                false
        );
        createCompensationTaskIfAbsent(
                RefundConstants.LATE_CALLBACK_REVIEW_BIZ_TYPE,
                paymentBill.getBillNo(),
                "Late callback manual review required for bizNo=" + paymentBill.getBizNo() + ", reason=" + effectiveReason.getCode()
        );
    }

    /**
     * 将账单标记为已支付（条件更新：仅 WAIT_PAY/PAYING -> SUCCESS）。
     * <p>
     * 更新支付状态、回调状态、第三方交易号，可选清除状态原因。
     * 当 publishBizSuccess 为 true 且账单确为新成功时，通过 Outbox 发布业务成功事件。
     * 若账单已被并发更新为终态（成功/关闭），重读真实状态后再决定是否仍需返回成功。
     */
    private void markBillPaid(PaymentBill paymentBill,
                              String thirdPartyBillNo,
                              String callbackStatus,
                              boolean publishBizSuccess,
                              String statusRemark,
                              boolean clearStatusReason) {
        if (PayStatusEnum.SUCCESS.name().equals(paymentBill.getPayStatus())) {
            return;
        }
        if (clearStatusReason) {
            clearStatusReason(paymentBill);
        }
        int updated;
        if (PayStatusEnum.CLOSED.name().equals(paymentBill.getPayStatus())) {
            // 迟到支付：只记录渠道成功事实，不走正常业务成功更新条件。
            updated = paymentBillMapper.markLatePaidIfClosed(
                    paymentBill.getBillNo(), callbackStatus, thirdPartyBillNo, statusRemark);
        } else {
            updated = paymentBillMapper.markPaidIfPending(
                    paymentBill.getBillNo(),
                    callbackStatus,
                    thirdPartyBillNo,
                    statusRemark,
                    paymentBill.getExtensionJson());
        }
        if (updated == 0) {
            // 并发：账单已被另一线程更新为终态，重读真实状态。
            PaymentBill latest = paymentBillMapper.selectOne(new LambdaQueryWrapper<PaymentBill>()
                    .eq(PaymentBill::getBillNo, paymentBill.getBillNo()));
            if (latest != null) {
                paymentBill.setPayStatus(latest.getPayStatus());
                paymentBill.setCallbackStatus(latest.getCallbackStatus());
                paymentBill.setThirdPartyBillNo(latest.getThirdPartyBillNo());
                paymentBill.setStatusRemark(latest.getStatusRemark());
                paymentBill.setExtensionJson(latest.getExtensionJson());
            }
            // 账单已被关闭 => 迟到回调路径由调用方按原因分流；
            // 账单已 SUCCESS => 幂等，下方不发 Outbox。
            return;
        }
        paymentBill.setPayStatus(PayStatusEnum.SUCCESS.name());
        paymentBill.setCallbackStatus(callbackStatus);
        paymentBill.setThirdPartyBillNo(thirdPartyBillNo);
        paymentBill.setStatusRemark(statusRemark);

        if (publishBizSuccess) {
            publishBizSuccess(paymentBill);
        }
    }

    /**
     * 通过 Outbox 发布业务支付成功事件。
     * <p>
     * 根据业务类型（RECHARGE / 其他）选择不同的消息队列。
     */
    private void publishBizSuccess(PaymentBill paymentBill) {
        String queueName = PaymentBizTypeEnum.RECHARGE.name().equals(paymentBill.getBizType())
                ? RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE
                : RabbitMQConfig.V1_ORDER_PAID_QUEUE;

        Map<String, Object> body = Map.of(
                "billNo", paymentBill.getBillNo(),
                "bizNo", paymentBill.getBizNo(),
                "bizType", paymentBill.getBizType()
        );

        MessageOutbox outbox = outboxPublisher.publish(OutboxMessageCommand.builder()
                .bizType(paymentBill.getBizType())
                .bizNo(paymentBill.getBizNo())
                .routingKey(queueName)
                .messageBody(body)
                .build());

        log.info("Outbox record inserted with PENDING status, bizNo={}, outboxId={}", paymentBill.getBizNo(), outbox.getId());
    }

    /**
     * 根据渠道编码查找对应的支付提供商。
     */
    private PaymentProvider getProvider(String channelCode) {
        Map<String, PaymentProvider> providerMap = paymentProviders.stream()
                .collect(Collectors.toMap(PaymentProvider::getChannelCode, Function.identity()));
        PaymentProvider paymentProvider = providerMap.get(channelCode);
        if (paymentProvider == null) {
            throw new BusinessException("支付渠道不存在");
        }
        return paymentProvider;
    }

    /**
     * 从账单扩展 JSON 中解析状态原因枚举。
     */
    private PaymentStatusReasonEnum resolveStatusReason(PaymentBill paymentBill) {
        if (paymentBill.getExtensionJson() == null || paymentBill.getExtensionJson().isBlank()) {
            return null;
        }
        try {
            ObjectNode extension = parseExtensionJson(paymentBill.getExtensionJson());
            if (extension == null) {
                return null;
            }
            return PaymentStatusReasonEnum.fromCode(extension.path(RefundConstants.EXTENSION_STATUS_REASON_CODE).asText(null));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将状态原因编码写入账单扩展 JSON。
     */
    private void applyStatusReason(PaymentBill paymentBill, PaymentStatusReasonEnum statusReason) {
        paymentBill.setExtensionJson(buildClosedStatusReasonJson(paymentBill, statusReason));
    }

    /**
     * 从账单扩展 JSON 中清除状态原因字段。
     */
    private void clearStatusReason(PaymentBill paymentBill) {
        ObjectNode extension = parseExtensionJson(paymentBill.getExtensionJson());
        extension.remove(RefundConstants.EXTENSION_STATUS_REASON_CODE);
        paymentBill.setExtensionJson(extension.isEmpty() ? null : JsonUtils.toJson(extension));
    }

    /**
     * 安全解析账单扩展 JSON 为 ObjectNode，解析失败返回空节点。
     */
    private ObjectNode parseExtensionJson(String extensionJson) {
        if (extensionJson == null || extensionJson.isBlank()) {
            return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        }
        try {
            ObjectNode parsed = (ObjectNode) JsonUtils.fromJsonTree(extensionJson);
            return parsed == null ? com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode() : parsed;
        } catch (Exception e) {
            return com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        }
    }

    /** 若补偿任务不存在则创建（幂等）。 */
    private void createCompensationTaskIfAbsent(String bizType, String bizNo, String remark) {
        compensationTaskFactory.createIfAbsent(bizType, bizNo, remark);
    }

    /** 返回参数列表中第一个非空值，全部为空时返回 null。 */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}


