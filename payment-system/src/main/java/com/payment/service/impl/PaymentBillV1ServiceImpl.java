package com.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.config.RabbitMQConfig;
import com.payment.dto.PayResponseDTO;
import com.payment.dto.PaymentCallbackDTO;
import com.payment.entity.MessageOutbox;
import com.payment.entity.PaymentBill;
import com.payment.entity.PaymentCallbackRecord;
import com.payment.enums.CallbackStatusEnum;
import com.payment.enums.MessageProcessStatusEnum;
import com.payment.enums.OutboxSendStatusEnum;
import com.payment.enums.PayStatusEnum;
import com.payment.enums.PaymentBizTypeEnum;
import com.payment.mapper.MessageOutboxMapper;
import com.payment.mapper.PaymentBillMapper;
import com.payment.mapper.PaymentCallbackRecordMapper;
import com.payment.service.PaymentBillV1Service;
import com.payment.service.PaymentProvider;
import com.payment.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一支付单服务。
 *
 * 这里把业务单和支付单拆开，回调只做验签、落库和异步投递。
 */
@Service
@RequiredArgsConstructor
public class PaymentBillV1ServiceImpl implements PaymentBillV1Service {

    private final PaymentBillMapper paymentBillMapper;
    private final PaymentCallbackRecordMapper callbackRecordMapper;
    private final MessageOutboxMapper messageOutboxMapper;
    private final RabbitTemplate rabbitTemplate;
    private final List<PaymentProvider> paymentProviders;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentBill createBill(String bizType, String bizNo, Long tenantId, Long platformUserId, BigDecimal payAmount) {
        PaymentBill paymentBill = new PaymentBill();
        paymentBill.setBillNo(BizNoGenerator.generate("PB"));
        paymentBill.setBizType(bizType);
        paymentBill.setBizNo(bizNo);
        paymentBill.setTenantId(tenantId);
        paymentBill.setPlatformUserId(platformUserId);
        paymentBill.setChannelCode("EXT_PROVIDER");
        paymentBill.setChannelMode("REDIRECT");
        paymentBill.setPayAmount(payAmount);
        paymentBill.setPayStatus(PayStatusEnum.WAIT_PAY.name());
        paymentBill.setCallbackStatus(CallbackStatusEnum.NOT_CALLBACK.name());
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

        paymentBill.setPayStatus(PayStatusEnum.SUCCESS.name());
        paymentBill.setCallbackStatus(CallbackStatusEnum.CALLBACK_SUCCESS.name());
        paymentBill.setThirdPartyBillNo(callbackDTO.getThirdPartyBillNo());
        paymentBill.setUpdateTime(LocalDateTime.now());
        paymentBillMapper.updateById(paymentBill);

        callbackRecord.setProcessStatus(MessageProcessStatusEnum.SUCCESS.name());
        callbackRecordMapper.updateById(callbackRecord);

        String queueName = PaymentBizTypeEnum.RECHARGE.name().equals(paymentBill.getBizType())
                ? RabbitMQConfig.V1_RECHARGE_SUCCESS_QUEUE
                : RabbitMQConfig.V1_ORDER_PAID_QUEUE;

        Map<String, Object> body = Map.of(
                "billNo", paymentBill.getBillNo(),
                "bizNo", paymentBill.getBizNo(),
                "bizType", paymentBill.getBizType()
        );

        MessageOutbox outbox = new MessageOutbox();
        outbox.setMessageId(BizNoGenerator.generate("MSG"));
        outbox.setBizType(paymentBill.getBizType());
        outbox.setBizNo(paymentBill.getBizNo());
        outbox.setExchangeName("");
        outbox.setRoutingKey(queueName);
        outbox.setMessageBody(JSON.toJSONString(body));
        outbox.setSendStatus(OutboxSendStatusEnum.PENDING.name());
        outbox.setRetryCount(0);
        messageOutboxMapper.insert(outbox);

        rabbitTemplate.convertAndSend(queueName, outbox.getMessageBody());
        outbox.setSendStatus(OutboxSendStatusEnum.SENT.name());
        messageOutboxMapper.updateById(outbox);
    }

    @Override
    public PaymentBill getByBillNo(String billNo) {
        return paymentBillMapper.selectOne(new LambdaQueryWrapper<PaymentBill>()
                .eq(PaymentBill::getBillNo, billNo));
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
}
