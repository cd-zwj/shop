package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.CompensationTask;
import com.payment.entity.RetryTask;
import com.payment.entity.SalesOrder;
import com.payment.mapper.CompensationTaskMapper;
import com.payment.mapper.RetryTaskMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.service.AppOrderService;
import com.payment.service.RefundService;
import com.payment.service.SmsCodeService;
import com.payment.service.CouponService;
import com.payment.service.WalletRechargeService;
import com.payment.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 重试任务调度器。
 * 轮询 retry_task 表中 PENDING 且 (nextRetryTime IS NULL OR nextRetryTime <= now) 的记录。
 *
 * 已接入的处理器：
 * - REFUND_QUERY   → RefundService（退款对账，委托 CompensationTask）
 * - ORDER_CLOSE    → AppOrderService.cancelOrder
 * - RECHARGE_CREDIT → WalletRechargeService.handleRechargeSuccess
 * - SMS_RETRY      → SmsCodeService.sendLoginCode
 *
 * - PAYMENT_CALLBACK  → AppOrderService.handlePaymentCallback
 * - COUPON_COMPENSATE → CouponService.receiveCoupon
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryTaskScheduler {

    private static final int BATCH_SIZE = 20;

    private final RetryTaskMapper retryTaskMapper;
    private final CompensationTaskMapper compensationTaskMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final RefundService refundService;
    private final AppOrderService appOrderService;
    private final WalletRechargeService walletRechargeService;
    private final SmsCodeService smsCodeService;
    private final CouponService couponService;

    @Scheduled(fixedDelayString = "${payment.retry-task.fixed-delay-ms:60000}")
    public void processRetryTasks() {
        LocalDateTime now = LocalDateTime.now();
        List<RetryTask> tasks = retryTaskMapper.selectList(new LambdaQueryWrapper<RetryTask>()
                .eq(RetryTask::getTaskStatus, "PENDING")
                .and(w -> w.isNull(RetryTask::getNextRetryTime).or().le(RetryTask::getNextRetryTime, now))
                .orderByAsc(RetryTask::getCreateTime)
                .last("LIMIT " + BATCH_SIZE));

        for (RetryTask task : tasks) {
            try {
                dispatch(task);
            } catch (Exception e) {
                log.error("重试任务执行失败: taskNo={}, taskType={}", task.getTaskNo(), task.getTaskType(), e);
                onFailure(task, e.getMessage());
            }
        }
    }

    private void dispatch(RetryTask task) {
        switch (task.getTaskType()) {
            case "REFUND_QUERY" -> handleRefundQuery(task);
            case "ORDER_CLOSE" -> handleOrderClose(task);
            case "RECHARGE_CREDIT" -> handleRechargeCredit(task);
            case "SMS_RETRY" -> handleSmsRetry(task);
            case "PAYMENT_CALLBACK" -> handlePaymentCallback(task);
            case "COUPON_COMPENSATE" -> handleCouponCompensate(task);
            default -> handleUnsupported(task);
        }
    }

    /* ---------- 已接入的处理器 ---------- */

    /**
     * REFUND_QUERY: 委托给已有的退款对账流程（通过 CompensationTask）。
     */
    private void handleRefundQuery(RetryTask task) {
        CompensationTask ct = compensationTaskMapper.selectOne(
                new LambdaQueryWrapper<CompensationTask>()
                        .eq(CompensationTask::getBizNo, task.getBizNo())
                        .eq(CompensationTask::getBizType, "LATE_CALLBACK_REFUND"));

        if (ct == null) {
            markDead(task, "关联的 CompensationTask 不存在: bizNo=" + task.getBizNo());
            return;
        }
        if ("SUCCESS".equals(ct.getTaskStatus())) {
            onSuccess(task, "退款补偿任务已完成");
            return;
        }
        if ("FAIL".equals(ct.getTaskStatus())) {
            markDead(task, "退款补偿任务已失败: " + ct.getRemark());
            return;
        }

        refundService.processLateCallbackRefundTask(ct);

        if ("SUCCESS".equals(ct.getTaskStatus())) {
            onSuccess(task, ct.getRemark());
        } else if ("FAIL".equals(ct.getTaskStatus())) {
            markDead(task, "退款处理失败: " + ct.getRemark());
        } else {
            onFailure(task, "退款处理中，等待下次重试");
        }
    }

    /**
     * ORDER_CLOSE: 关闭超时未支付订单。
     * bizNo = orderNo，通过 SalesOrder 查 platformUserId。
     */
    private void handleOrderClose(RetryTask task) {
        SalesOrder order = salesOrderMapper.selectOne(
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getOrderNo, task.getBizNo())
                        .eq(SalesOrder::getDeleted, 0));
        if (order == null) {
            markDead(task, "订单不存在: " + task.getBizNo());
            return;
        }
        appOrderService.cancelOrder(order.getPlatformUserId(), task.getBizNo());
        onSuccess(task, "订单已关闭: " + task.getBizNo());
    }

    /**
     * RECHARGE_CREDIT: 充值入账重试。
     * bizNo = rechargeNo。
     */
    private void handleRechargeCredit(RetryTask task) {
        walletRechargeService.handleRechargeSuccess(task.getBizNo());
        onSuccess(task, "充值入账成功: " + task.getBizNo());
    }

    /**
     * SMS_RETRY: 短信重发。
     * 从 extensionJson 中解析手机号：{"phone": "13800138000"}
     */
    @SuppressWarnings("unchecked")
    private void handleSmsRetry(RetryTask task) {
        if (task.getExtensionJson() == null || task.getExtensionJson().isBlank()) {
            markDead(task, "extensionJson 为空，无法解析手机号");
            return;
        }
        Map<String, Object> ext = JsonUtils.fromJson(task.getExtensionJson(), Map.class);
        String phone = ext != null ? String.valueOf(ext.get("phone")) : null;
        if (phone == null || phone.isBlank()) {
            markDead(task, "extensionJson 中无 phone 字段");
            return;
        }
        smsCodeService.sendLoginCode(phone);
        onSuccess(task, "短信已重发: " + phone);
    }


    /**
     * PAYMENT_CALLBACK: 支付回调重试，同步支付单状态并更新订单。
     * bizNo = paymentBillNo。
     */
    private void handlePaymentCallback(RetryTask task) {
        try {
            appOrderService.handlePaymentCallback(task.getBizNo());
            onSuccess(task, "支付回调处理成功: " + task.getBizNo());
        } catch (Exception e) {
            log.warn("支付回调处理失败，等待重试: taskNo={}, bizNo={}", task.getTaskNo(), task.getBizNo(), e);
            onFailure(task, e.getMessage());
        }
    }

    /**
     * COUPON_COMPENSATE: 优惠券发放补偿重试。
     * extensionJson 包含 couponTemplateId, tenantId, platformUserId。
     */
    @SuppressWarnings("unchecked")
    private void handleCouponCompensate(RetryTask task) {
        if (task.getExtensionJson() == null || task.getExtensionJson().isBlank()) {
            markDead(task, "extensionJson 为空，无法解析优惠券发放参数");
            return;
        }
        Map<String, Object> ext = JsonUtils.fromJson(task.getExtensionJson(), Map.class);
        if (ext == null || !ext.containsKey("couponTemplateId") || !ext.containsKey("tenantId") || !ext.containsKey("platformUserId")) {
            markDead(task, "extensionJson 缺少必填字段 couponTemplateId/tenantId/platformUserId");
            return;
        }
        Long couponTemplateId = ((Number) ext.get("couponTemplateId")).longValue();
        Long tenantId = ((Number) ext.get("tenantId")).longValue();
        Long platformUserId = ((Number) ext.get("platformUserId")).longValue();
        couponService.receiveCoupon(couponTemplateId, tenantId, platformUserId, task.getBizNo());
        onSuccess(task, "优惠券补偿发放成功: bizNo=" + task.getBizNo());
    }
    /* ---------- 通用方法 ---------- */

    /**
     * 未接入的 taskType：退避重试，超 maxRetryCount 后 DEAD。
     */
    private void handleUnsupported(RetryTask task) {
        log.warn("重试任务暂无处理器，退避重试: taskNo={}, taskType={}", task.getTaskNo(), task.getTaskType());
        onFailure(task, "暂无对应处理器: " + task.getTaskType());
    }

    private void onSuccess(RetryTask task, String message) {
        task.setTaskStatus("SUCCESS");
        task.setLastErrorMessage(null);
        task.setUpdateTime(LocalDateTime.now());
        retryTaskMapper.updateById(task);
        log.info("重试任务成功: taskNo={}", task.getTaskNo());
    }

    private void onFailure(RetryTask task, String message) {
        int count = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;
        task.setRetryCount(count);
        task.setLastErrorMessage(message);

        if (count >= (task.getMaxRetryCount() == null ? 16 : task.getMaxRetryCount())) {
            task.setTaskStatus("DEAD");
            log.warn("重试任务达到最大重试次数，标记为 DEAD: taskNo={}, retryCount={}", task.getTaskNo(), count);
        } else {
            task.setTaskStatus("PENDING");
            task.setNextRetryTime(LocalDateTime.now().plusMinutes(Math.min(count, 5)));
        }
        task.setUpdateTime(LocalDateTime.now());
        retryTaskMapper.updateById(task);
    }

    private void markDead(RetryTask task, String reason) {
        task.setTaskStatus("DEAD");
        task.setLastErrorMessage(reason);
        task.setUpdateTime(LocalDateTime.now());
        retryTaskMapper.updateById(task);
    }
}
