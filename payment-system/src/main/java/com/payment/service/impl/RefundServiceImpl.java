package com.payment.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.constant.RefundConstants;
import com.payment.common.BusinessException;
import com.payment.dto.RefundQueryResultDTO;
import com.payment.dto.RefundRequestDTO;
import com.payment.dto.RefundSubmitResultDTO;
import com.payment.entity.CompensationTask;
import com.payment.entity.PaymentBill;
import com.payment.entity.RefundOrder;
import com.payment.entity.RefundRecord;
import com.payment.entity.RefundReconcileTask;
import com.payment.enums.RefundAuditStatusEnum;
import com.payment.enums.RefundChannelStatusEnum;
import com.payment.enums.RefundReconcileTaskStatusEnum;
import com.payment.enums.RefundStatusEnum;
import com.payment.enums.PaymentStatusReasonEnum;
import com.payment.mapper.CompensationTaskMapper;
import com.payment.mapper.PaymentBillMapper;
import com.payment.mapper.RefundOrderMapper;
import com.payment.mapper.RefundRecordMapper;
import com.payment.mapper.RefundReconcileTaskMapper;
import com.payment.service.PaymentProvider;
import com.payment.service.RefundService;
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
public class RefundServiceImpl implements RefundService {
    private static final int MAX_LATE_CALLBACK_REFUND_RETRY_COUNT = 5;

    private final RefundOrderMapper refundOrderMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final RefundReconcileTaskMapper refundReconcileTaskMapper;
    private final CompensationTaskMapper compensationTaskMapper;
    private final PaymentBillMapper paymentBillMapper;
    private final List<PaymentProvider> paymentProviders;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void prepareLateCallbackRefund(PaymentBill paymentBill, PaymentStatusReasonEnum statusReason) {
        RefundOrder refundOrder = refundOrderMapper.selectOne(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getPaymentBillNo, paymentBill.getBillNo())
                .eq(RefundOrder::getDeleted, 0));
        if (refundOrder == null) {
            refundOrder = new RefundOrder();
            refundOrder.setRefundNo(BizNoGenerator.generate("RF"));
            refundOrder.setBizType(paymentBill.getBizType());
            refundOrder.setBizNo(paymentBill.getBizNo());
            refundOrder.setTenantId(paymentBill.getTenantId());
            refundOrder.setPlatformUserId(paymentBill.getPlatformUserId());
            refundOrder.setOrderNo(paymentBill.getBizNo());
            refundOrder.setPaymentBillNo(paymentBill.getBillNo());
            refundOrder.setChannelCode(paymentBill.getChannelCode());
            refundOrder.setRefundReason(statusReason.getRemark());
            refundOrder.setApplyAmount(paymentBill.getPayAmount());
            refundOrder.setRefundAmount(paymentBill.getPayAmount());
            refundOrder.setWalletRefundAmount(BigDecimal.ZERO);
            refundOrder.setExternalRefundAmount(paymentBill.getPayAmount());
            refundOrder.setRefundStatus(RefundStatusEnum.APPLIED.name());
            refundOrder.setAuditStatus(RefundAuditStatusEnum.APPROVED.name());
            refundOrder.setAuditTime(LocalDateTime.now());
            refundOrder.setDeleted(0);
            refundOrderMapper.insert(refundOrder);
        }

        RefundRecord refundRecord = refundRecordMapper.selectOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundNo, refundOrder.getRefundNo())
                .eq(RefundRecord::getChannelCode, refundOrder.getChannelCode()));
        if (refundRecord == null) {
            refundRecord = new RefundRecord();
            refundRecord.setRefundNo(refundOrder.getRefundNo());
            refundRecord.setPaymentBillNo(refundOrder.getPaymentBillNo());
            refundRecord.setChannelCode(refundOrder.getChannelCode());
            refundRecord.setRefundAmount(refundOrder.getExternalRefundAmount());
            refundRecord.setThirdPartyBillNo(paymentBill.getThirdPartyBillNo());
            refundRecord.setChannelStatus(RefundChannelStatusEnum.PROCESSING.name());
            refundRecord.setNotifyData(buildAuditPayload("PREPARED", statusReason.getCode(), statusReason.getRemark()));
            refundRecordMapper.insert(refundRecord);
        }

        createLateCallbackRefundTaskIfAbsent(paymentBill.getBillNo(), refundOrder.getRefundNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processLateCallbackRefundTask(CompensationTask compensationTask) {
        RefundOrder refundOrder = refundOrderMapper.selectOne(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getPaymentBillNo, compensationTask.getBizNo())
                .eq(RefundOrder::getDeleted, 0));
        if (refundOrder == null) {
            markCompensationTaskFailed(compensationTask, "Refund order not found");
            return;
        }
        if (RefundStatusEnum.SUCCESS.name().equals(refundOrder.getRefundStatus())) {
            markCompensationTaskSuccess(compensationTask, "Refund already completed");
            return;
        }

        PaymentBill paymentBill = paymentBillMapper.selectOne(new LambdaQueryWrapper<PaymentBill>()
                .eq(PaymentBill::getBillNo, refundOrder.getPaymentBillNo()));
        if (paymentBill == null) {
            markRefundFailure(refundOrder, null, "Payment bill not found");
            markCompensationTaskFailed(compensationTask, "Payment bill not found");
            return;
        }

        RefundRecord refundRecord = refundRecordMapper.selectOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundNo, refundOrder.getRefundNo())
                .eq(RefundRecord::getChannelCode, refundOrder.getChannelCode()));
        if (refundRecord == null) {
            refundRecord = new RefundRecord();
            refundRecord.setRefundNo(refundOrder.getRefundNo());
            refundRecord.setPaymentBillNo(refundOrder.getPaymentBillNo());
            refundRecord.setChannelCode(refundOrder.getChannelCode());
            refundRecord.setRefundAmount(refundOrder.getExternalRefundAmount());
            refundRecord.setThirdPartyBillNo(paymentBill.getThirdPartyBillNo());
            refundRecord.setChannelStatus(RefundChannelStatusEnum.PROCESSING.name());
            refundRecordMapper.insert(refundRecord);
        }

        if (RefundStatusEnum.PROCESSING.name().equals(refundOrder.getRefundStatus())
                || (refundRecord.getThirdPartyRefundNo() != null && !refundRecord.getThirdPartyRefundNo().isBlank())
                || hasReconcileTask(refundOrder.getRefundNo(), refundOrder.getChannelCode())) {
            createRefundReconcileTaskIfAbsent(refundOrder.getRefundNo(), refundOrder.getChannelCode());
            markCompensationTaskSuccess(compensationTask, "Refund already submitted and waiting for reconciliation");
            return;
        }

        PaymentProvider provider = resolveProvider(refundOrder.getChannelCode());
        if (!provider.supportsRefund()) {
            markRefundFailure(refundOrder, refundRecord, "Provider refund is not supported in phase 1");
            markCompensationTaskFailed(compensationTask, "Provider refund is not supported in phase 1");
            return;
        }

        compensationTask.setTaskStatus("PROCESSING");
        compensationTaskMapper.updateById(compensationTask);

        RefundRequestDTO requestDTO = new RefundRequestDTO();
        requestDTO.setRefundNo(refundOrder.getRefundNo());
        requestDTO.setRefundAmount(refundOrder.getExternalRefundAmount());
        requestDTO.setRefundReason(refundOrder.getRefundReason());

        RefundSubmitResultDTO submitResult = provider.refund(paymentBill, requestDTO);
        refundRecord.setThirdPartyBillNo(paymentBill.getThirdPartyBillNo());
        refundRecord.setThirdPartyRefundNo(submitResult.getProviderRefundNo());
        refundRecord.setNotifyData(buildProviderPayload(submitResult.getRawStatus(), submitResult.getMessage()));
        refundRecordMapper.updateById(refundRecord);

        if (RefundChannelStatusEnum.SUCCESS.name().equals(submitResult.getChannelStatus())) {
            markRefundSuccess(refundOrder, refundRecord, "Refund completed by provider");
            markCompensationTaskSuccess(compensationTask, "Refund completed");
            return;
        }

        if (RefundChannelStatusEnum.PROCESSING.name().equals(submitResult.getChannelStatus())) {
            refundOrder.setRefundStatus(RefundStatusEnum.PROCESSING.name());
            refundOrderMapper.updateById(refundOrder);
            refundRecord.setChannelStatus(RefundChannelStatusEnum.PROCESSING.name());
            refundRecordMapper.updateById(refundRecord);
            createRefundReconcileTaskIfAbsent(refundOrder.getRefundNo(), refundOrder.getChannelCode());
            markCompensationTaskSuccess(compensationTask, "Refund submitted and waiting for reconciliation");
            return;
        }

        retryOrFailCompensationTask(compensationTask, refundOrder, refundRecord, submitResult.getMessage());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processRefundReconcileTask(RefundReconcileTask reconcileTask) {
        RefundOrder refundOrder = refundOrderMapper.selectOne(new LambdaQueryWrapper<RefundOrder>()
                .eq(RefundOrder::getRefundNo, reconcileTask.getRefundNo())
                .eq(RefundOrder::getDeleted, 0));
        RefundRecord refundRecord = refundRecordMapper.selectOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundNo, reconcileTask.getRefundNo())
                .eq(RefundRecord::getChannelCode, reconcileTask.getChannelCode()));

        if (refundOrder == null || refundRecord == null) {
            reconcileTask.setTaskStatus(RefundReconcileTaskStatusEnum.FAIL.name());
            reconcileTask.setLastResult("Refund order or record not found");
            refundReconcileTaskMapper.updateById(reconcileTask);
            return;
        }
        if (RefundStatusEnum.SUCCESS.name().equals(refundOrder.getRefundStatus())) {
            reconcileTask.setTaskStatus(RefundReconcileTaskStatusEnum.SUCCESS.name());
            reconcileTask.setLastResult("Refund already completed");
            refundReconcileTaskMapper.updateById(reconcileTask);
            return;
        }

        PaymentProvider provider = resolveProvider(refundRecord.getChannelCode());
        if (!provider.supportsRefund()) {
            markRefundFailure(refundOrder, refundRecord, "Provider refund query is not supported");
            reconcileTask.setTaskStatus(RefundReconcileTaskStatusEnum.FAIL.name());
            reconcileTask.setLastResult("Provider refund query is not supported");
            refundReconcileTaskMapper.updateById(reconcileTask);
            return;
        }

        reconcileTask.setTaskStatus(RefundReconcileTaskStatusEnum.PROCESSING.name());
        refundReconcileTaskMapper.updateById(reconcileTask);

        RefundQueryResultDTO queryResult = provider.queryRefund(refundRecord);
        refundRecord.setNotifyData(buildProviderPayload(queryResult.getRawStatus(), queryResult.getMessage()));
        if (queryResult.getProviderRefundNo() != null) {
            refundRecord.setThirdPartyRefundNo(queryResult.getProviderRefundNo());
        }
        refundRecordMapper.updateById(refundRecord);

        if (RefundChannelStatusEnum.SUCCESS.name().equals(queryResult.getChannelStatus())) {
            markRefundSuccess(refundOrder, refundRecord, "Refund confirmed by reconciliation");
            reconcileTask.setTaskStatus(RefundReconcileTaskStatusEnum.SUCCESS.name());
            reconcileTask.setLastResult("Refund confirmed");
            refundReconcileTaskMapper.updateById(reconcileTask);
            return;
        }

        retryOrFailReconcileTask(reconcileTask, refundOrder, refundRecord, queryResult.getMessage());
    }

    private PaymentProvider resolveProvider(String channelCode) {
        Map<String, PaymentProvider> providerMap = paymentProviders.stream()
                .collect(Collectors.toMap(PaymentProvider::getChannelCode, Function.identity()));
        PaymentProvider provider = providerMap.get(channelCode);
        if (provider == null) {
            throw new BusinessException("Payment provider not found: " + channelCode);
        }
        return provider;
    }

    private void createLateCallbackRefundTaskIfAbsent(String paymentBillNo, String refundNo) {
        CompensationTask existing = compensationTaskMapper.selectOne(new LambdaQueryWrapper<CompensationTask>()
                .eq(CompensationTask::getBizType, RefundConstants.LATE_CALLBACK_REFUND_BIZ_TYPE)
                .eq(CompensationTask::getBizNo, paymentBillNo));
        if (existing != null) {
            return;
        }

        CompensationTask task = new CompensationTask();
        task.setTaskNo(BizNoGenerator.generate("CT"));
        task.setBizType(RefundConstants.LATE_CALLBACK_REFUND_BIZ_TYPE);
        task.setBizNo(paymentBillNo);
        task.setTaskStatus("PENDING");
        task.setRemark("Late callback refund pending, refundNo=" + refundNo);
        task.setRetryCount(0);
        compensationTaskMapper.insert(task);
    }

    private void createRefundReconcileTaskIfAbsent(String refundNo, String channelCode) {
        RefundReconcileTask existing = refundReconcileTaskMapper.selectOne(new LambdaQueryWrapper<RefundReconcileTask>()
                .eq(RefundReconcileTask::getRefundNo, refundNo)
                .eq(RefundReconcileTask::getChannelCode, channelCode));
        if (existing != null) {
            return;
        }

        RefundReconcileTask task = new RefundReconcileTask();
        task.setTaskNo(BizNoGenerator.generate("RQ"));
        task.setRefundNo(refundNo);
        task.setChannelCode(channelCode);
        task.setTaskStatus(RefundReconcileTaskStatusEnum.PENDING.name());
        task.setRetryCount(0);
        task.setMaxRetryCount(10);
        task.setNextRetryTime(LocalDateTime.now().plusMinutes(1));
        refundReconcileTaskMapper.insert(task);
    }

    private boolean hasReconcileTask(String refundNo, String channelCode) {
        return refundReconcileTaskMapper.selectOne(new LambdaQueryWrapper<RefundReconcileTask>()
                .eq(RefundReconcileTask::getRefundNo, refundNo)
                .eq(RefundReconcileTask::getChannelCode, channelCode)) != null;
    }

    private void markRefundSuccess(RefundOrder refundOrder, RefundRecord refundRecord, String message) {
        refundOrder.setRefundStatus(RefundStatusEnum.SUCCESS.name());
        refundOrder.setSuccessTime(LocalDateTime.now());
        refundOrder.setFailReason(null);
        refundOrderMapper.updateById(refundOrder);

        if (refundRecord != null) {
            refundRecord.setChannelStatus(RefundChannelStatusEnum.SUCCESS.name());
            refundRecord.setSuccessTime(LocalDateTime.now());
            refundRecord.setNotifyData(buildProviderPayload(RefundChannelStatusEnum.SUCCESS.name(), message));
            refundRecordMapper.updateById(refundRecord);
        }
    }

    private void markRefundFailure(RefundOrder refundOrder, RefundRecord refundRecord, String message) {
        refundOrder.setRefundStatus(RefundStatusEnum.FAIL.name());
        refundOrder.setFailReason(message);
        refundOrderMapper.updateById(refundOrder);

        if (refundRecord != null) {
            refundRecord.setChannelStatus(RefundChannelStatusEnum.FAIL.name());
            refundRecord.setNotifyData(buildProviderPayload(RefundChannelStatusEnum.FAIL.name(), message));
            refundRecordMapper.updateById(refundRecord);
        }
    }

    private void retryOrFailCompensationTask(CompensationTask task, RefundOrder refundOrder, RefundRecord refundRecord, String message) {
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        retryCount++;
        task.setRetryCount(retryCount);
        task.setRemark(message);
        if (retryCount >= MAX_LATE_CALLBACK_REFUND_RETRY_COUNT) {
            task.setTaskStatus("FAIL");
            markRefundFailure(refundOrder, refundRecord, message);
        } else {
            task.setTaskStatus("PENDING");
        }
        compensationTaskMapper.updateById(task);
    }

    private void retryOrFailReconcileTask(RefundReconcileTask task, RefundOrder refundOrder, RefundRecord refundRecord, String message) {
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        retryCount++;
        task.setRetryCount(retryCount);
        task.setLastResult(message);
        if (retryCount >= (task.getMaxRetryCount() == null ? 10 : task.getMaxRetryCount())) {
            task.setTaskStatus(RefundReconcileTaskStatusEnum.FAIL.name());
            markRefundFailure(refundOrder, refundRecord, message);
        } else {
            task.setTaskStatus(RefundReconcileTaskStatusEnum.PENDING.name());
            task.setNextRetryTime(LocalDateTime.now().plusMinutes(Math.min(retryCount, 5)));
        }
        refundReconcileTaskMapper.updateById(task);
    }

    private void markCompensationTaskSuccess(CompensationTask task, String message) {
        task.setTaskStatus("SUCCESS");
        task.setRemark(message);
        compensationTaskMapper.updateById(task);
    }

    private void markCompensationTaskFailed(CompensationTask task, String message) {
        task.setTaskStatus("FAIL");
        task.setRemark(message);
        compensationTaskMapper.updateById(task);
    }

    private String buildAuditPayload(String status, String reasonCode, String reason) {
        return JSON.toJSONString(Map.of(
                "status", status,
                "reasonCode", reasonCode,
                "reason", reason
        ));
    }

    private String buildProviderPayload(String rawStatus, String message) {
        return JSON.toJSONString(Map.of(
                "rawStatus", rawStatus == null ? "" : rawStatus,
                "message", message == null ? "" : message
        ));
    }
}
