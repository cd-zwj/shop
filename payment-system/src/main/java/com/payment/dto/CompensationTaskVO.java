package com.payment.dto;

import com.payment.entity.CompensationTask;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CompensationTaskVO {
    private Long id;
    private String taskNo;
    private String bizType;
    private String bizNo;
    private String taskStatus;
    private Integer retryCount;
    private Integer maxRetryCount;
    private LocalDateTime nextRetryTime;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static CompensationTaskVO from(CompensationTask task) {
        if (task == null) {
            return null;
        }
        return CompensationTaskVO.builder()
                .id(task.getId())
                .taskNo(task.getTaskNo())
                .bizType(task.getBizType())
                .bizNo(task.getBizNo())
                .taskStatus(task.getTaskStatus())
                .retryCount(task.getRetryCount())
                .maxRetryCount(null)
                .nextRetryTime(null)
                .remark(task.getRemark())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
    }
}
