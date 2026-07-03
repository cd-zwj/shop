package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDeleteTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskId;
    private String filename;
    private String fileHash;
    private String userId;
    private String minioPath;
    private List<String> vectorIds;
    private List<String> unitIds;

    @Builder.Default
    private int retryCount = 0;

    @Builder.Default
    private int maxRetries = 3;

    private Long createTimestamp;

    @Builder.Default
    private StepStatus redisStatus = StepStatus.PENDING;

    @Builder.Default
    private StepStatus mysqlStatus = StepStatus.PENDING;

    @Builder.Default
    private StepStatus minioStatus = StepStatus.PENDING;

    public enum StepStatus {
        PENDING,
        SUCCESS,
        FAILED
    }

    public boolean needsRetry() {
        return retryCount < maxRetries &&
                (redisStatus != StepStatus.SUCCESS
                        || mysqlStatus != StepStatus.SUCCESS
                        || minioStatus != StepStatus.SUCCESS);
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public boolean isAllSuccess() {
        return redisStatus == StepStatus.SUCCESS
                && mysqlStatus == StepStatus.SUCCESS
                && minioStatus == StepStatus.SUCCESS;
    }
}
