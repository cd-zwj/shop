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
import com.payment.enums.CallbackStatusEnum;
import com.payment.enums.MessageProcessStatusEnum;
import com.payment.enums.PayStatusEnum;
import com.payment.enums.PaymentChannelCodeEnum;
import com.payment.enums.PaymentLateCallbackActionEnum;
import com.payment.enums.PaymentStatusReasonEnum;
import com.payment.enums.PaymentBizTypeEnum;
import com.payment.mapper.PaymentBillMapper;
import com.payment.mapper.PaymentCallbackRecordMapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentBillV1ServiceImpl implements PaymentBillV1Service {

    private final PaymentBillMapper paymentBillMapper;
    private final PaymentCallbackRecordMapper callbackRecordMapper;
    private final CompensationTaskFactory compensationTaskFactory;
    private final OutboxPublisher outboxPublisher;
    private final List<PaymentProvider> paymentProviders;
    private final RefundService refundService;

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

    @Override
    public PayResponseDTO createExternalPayment(PaymentBill paymentBill) {
        return getProvider(paymentBill.getChannelCode()).createPayment(paymentBill);
    }

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

    private void closePaymentBill(PaymentBill paymentBill, PaymentStatusReasonEnum statusReason) {
        if (PayStatusEnum.SUCCESS.name().equals(paymentBill.getPayStatus())) {
            return;
        }

        paymentBill.setPayStatus(PayStatusEnum.CLOSED.name());
        paymentBill.setStatusRemark(statusReason.getRemark());
        applyStatusReason(paymentBill, statusReason);
        paymentBill.setUpdateTime(LocalDateTime.now());
        paymentBillMapper.updateById(paymentBill);
    }

    @Override
    public PaymentBill getByBillNo(String billNo) {
        return paymentBillMapper.selectOne(new LambdaQueryWrapper<PaymentBill>()
                .eq(PaymentBill::getBillNo, billNo));
    }

    @Override
    public PaymentBill getLatestByBizTypeAndBizNo(String bizType, String bizNo) {
        List<PaymentBill> paymentBills = listByBizTypeAndBizNo(bizType, bizNo);
        return paymentBills.isEmpty() ? null : paymentBills.get(0);
    }

    @Override
    public List<PaymentBill> listByBizTypeAndBizNo(String bizType, String bizNo) {
        return paymentBillMapper.selectList(new LambdaQueryWrapper<PaymentBill>()
                .eq(PaymentBill::getBizType, bizType)
                .eq(PaymentBill::getBizNo, bizNo)
                .orderByDesc(PaymentBill::getCreateTime)
                .orderByDesc(PaymentBill::getId));
    }

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

    private void handlePaidResult(PaymentBill paymentBill, String thirdPartyBillNo, String callbackStatus) {
        if (PayStatusEnum.SUCCESS.name().equals(paymentBill.getPayStatus())) {
            return;
        }

        PaymentStatusReasonEnum statusReason = resolveStatusReason(paymentBill);
        if (PayStatusEnum.CLOSED.name().equals(paymentBill.getPayStatus())) {
            handleClosedBillLateSuccess(paymentBill, thirdPartyBillNo, callbackStatus, statusReason);
            return;
        }

        markBillPaid(paymentBill, thirdPartyBillNo, callbackStatus, true, null, true);
    }

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

    private void markBillPaid(PaymentBill paymentBill,
                              String thirdPartyBillNo,
                              String callbackStatus,
                              boolean publishBizSuccess,
                              String statusRemark,
                              boolean clearStatusReason) {
        if (PayStatusEnum.SUCCESS.name().equals(paymentBill.getPayStatus())) {
            return;
        }

        paymentBill.setPayStatus(PayStatusEnum.SUCCESS.name());
        paymentBill.setCallbackStatus(callbackStatus);
        paymentBill.setThirdPartyBillNo(thirdPartyBillNo);
        paymentBill.setStatusRemark(statusRemark);
        if (clearStatusReason) {
            clearStatusReason(paymentBill);
        }
        paymentBill.setUpdateTime(LocalDateTime.now());
        paymentBillMapper.updateById(paymentBill);

        if (publishBizSuccess) {
            publishBizSuccess(paymentBill);
        }
    }

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

    private PaymentProvider getProvider(String channelCode) {
        Map<String, PaymentProvider> providerMap = paymentProviders.stream()
                .collect(Collectors.toMap(PaymentProvider::getChannelCode, Function.identity()));
        PaymentProvider paymentProvider = providerMap.get(channelCode);
        if (paymentProvider == null) {
            throw new BusinessException("支付渠道不存在");
        }
        return paymentProvider;
    }

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

    private void applyStatusReason(PaymentBill paymentBill, PaymentStatusReasonEnum statusReason) {
        ObjectNode extension = parseExtensionJson(paymentBill.getExtensionJson());
        extension.put(RefundConstants.EXTENSION_STATUS_REASON_CODE, statusReason.getCode());
        paymentBill.setExtensionJson(JsonUtils.toJson(extension));
    }

    private void clearStatusReason(PaymentBill paymentBill) {
        ObjectNode extension = parseExtensionJson(paymentBill.getExtensionJson());
        extension.remove(RefundConstants.EXTENSION_STATUS_REASON_CODE);
        paymentBill.setExtensionJson(extension.isEmpty() ? null : JsonUtils.toJson(extension));
    }

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

    private void createCompensationTaskIfAbsent(String bizType, String bizNo, String remark) {
        compensationTaskFactory.createIfAbsent(bizType, bizNo, remark);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}


