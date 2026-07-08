package com.payment.dto;

import com.payment.entity.CompensationTask;
import com.payment.entity.RetryTask;
import com.payment.vo.VoConverterUtil;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

/**
 * 商户工作台可见的系统任务，只读展示，不暴露管理员操作能力。
 */
@Value
@Builder
public class MerchantWorkbenchTaskVO implements Serializable {
    String taskSource;
    Long id;
    String taskNo;
    String taskType;
    String bizType;
    String bizNo;
    String taskStatus;
    Integer retryCount;
    Integer maxRetryCount;
    String nextRetryTime;
    String lastError;
    String createTime;
    String updateTime;
    String actionLabel;
    String actionPath;

    public static MerchantWorkbenchTaskVO fromCompensation(CompensationTask task) {
        return MerchantWorkbenchTaskVO.builder()
                .taskSource("compensation")
                .id(task.getId())
                .taskNo(task.getTaskNo())
                .taskType(task.getBizType())
                .bizType(task.getBizType())
                .bizNo(task.getBizNo())
                .taskStatus(task.getTaskStatus())
                .retryCount(task.getRetryCount())
                .lastError(task.getRemark())
                .createTime(VoConverterUtil.formatTime(task.getCreateTime()))
                .updateTime(VoConverterUtil.formatTime(task.getUpdateTime()))
                .actionLabel(resolveActionLabel(task.getBizType()))
                .actionPath(resolveActionPath(task.getBizType()))
                .build();
    }

    public static MerchantWorkbenchTaskVO fromRetry(RetryTask task) {
        return MerchantWorkbenchTaskVO.builder()
                .taskSource("retry")
                .id(task.getId())
                .taskNo(task.getTaskNo())
                .taskType(task.getTaskType())
                .bizType(task.getBizType())
                .bizNo(task.getBizNo())
                .taskStatus(task.getTaskStatus())
                .retryCount(task.getRetryCount())
                .maxRetryCount(task.getMaxRetryCount())
                .nextRetryTime(VoConverterUtil.formatTime(task.getNextRetryTime()))
                .lastError(task.getLastErrorMessage())
                .createTime(VoConverterUtil.formatTime(task.getCreateTime()))
                .updateTime(VoConverterUtil.formatTime(task.getUpdateTime()))
                .actionLabel(resolveActionLabel(task.getBizType()))
                .actionPath(resolveActionPath(task.getBizType()))
                .build();
    }

    private static String resolveActionLabel(String bizType) {
        if ("MERCHANT_APPROVED_REFUND".equals(bizType) || "LATE_CALLBACK_REFUND".equals(bizType)) {
            return "查看退款单";
        }
        return "查看异常订单";
    }

    private static String resolveActionPath(String bizType) {
        if ("MERCHANT_APPROVED_REFUND".equals(bizType) || "LATE_CALLBACK_REFUND".equals(bizType)) {
            return "/merchant/refunds?status=FAILED";
        }
        return "/merchant/orders?tab=abnormal";
    }
}
