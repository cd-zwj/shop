package com.payment.dto;

import com.payment.entity.RetryTask;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RetryTaskVO {
    private Long id;
    private String taskNo;
    private String taskType;
    private String bizNo;
    private String taskStatus;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime nextRetryTime;
    private String lastError;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static RetryTaskVO from(RetryTask task) {
        if (task == null) {
            return null;
        }
        return RetryTaskVO.builder()
                .id(task.getId())
                .taskNo(task.getTaskNo())
                .taskType(task.getTaskType())
                .bizNo(task.getBizNo())
                .taskStatus(task.getTaskStatus())
                .retryCount(task.getRetryCount())
                .maxRetryCount(task.getMaxRetryCount())
                .nextRetryTime(task.getNextRetryTime())
                .lastError(task.getLastErrorMessage())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
    }
}
