package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除任务状态查询响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteTaskStatus {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 整体状态
     */
    private OverallStatus status;

    /**
     * Redis删除状态
     */
    private String redisStatus;

    /**
     * MySQL删除状态
     */
    private String mysqlStatus;

    /**
     * MinIO删除状态
     */
    private String minioStatus;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 整体状态枚举
     */
    public enum OverallStatus {
        PROCESSING,   // 处理中
        SUCCESS,      // 全部成功
        PARTIAL,      // 部分成功
        FAILED        // 全部失败
    }

    /**
     * 创建处理中的状态
     */
    public static DeleteTaskStatus processing(String taskId, String filename) {
        return DeleteTaskStatus.builder()
                .taskId(taskId)
                .filename(filename)
                .status(OverallStatus.PROCESSING)
                .redisStatus("PENDING")
                .mysqlStatus("PENDING")
                .minioStatus("PENDING")
                .retryCount(0)
                .build();
    }

    /**
     * 从任务创建状态
     */
    public static DeleteTaskStatus fromTask(FileDeleteTask task) {
        OverallStatus overallStatus;
        if (task.isAllSuccess()) {
            overallStatus = OverallStatus.SUCCESS;
        } else if (task.getRetryCount() >= task.getMaxRetries()) {
            // 已达到最大重试次数
            if (task.getRedisStatus() == FileDeleteTask.StepStatus.FAILED &&
                task.getMysqlStatus() == FileDeleteTask.StepStatus.FAILED &&
                task.getMinioStatus() == FileDeleteTask.StepStatus.FAILED) {
                overallStatus = OverallStatus.FAILED;
            } else {
                overallStatus = OverallStatus.PARTIAL;
            }
        } else {
            overallStatus = OverallStatus.PROCESSING;
        }

        StringBuilder errorMsg = new StringBuilder();
        if (task.getRedisStatus() == FileDeleteTask.StepStatus.FAILED) {
            errorMsg.append("Redis删除失败; ");
        }
        if (task.getMysqlStatus() == FileDeleteTask.StepStatus.FAILED) {
            errorMsg.append("MySQL删除失败; ");
        }
        if (task.getMinioStatus() == FileDeleteTask.StepStatus.FAILED) {
            errorMsg.append("MinIO删除失败; ");
        }

        return DeleteTaskStatus.builder()
                .taskId(task.getTaskId())
                .filename(task.getFilename())
                .status(overallStatus)
                .redisStatus(task.getRedisStatus().name())
                .mysqlStatus(task.getMysqlStatus().name())
                .minioStatus(task.getMinioStatus().name())
                .retryCount(task.getRetryCount())
                .errorMessage(errorMsg.length() > 0 ? errorMsg.toString() : null)
                .build();
    }
}
