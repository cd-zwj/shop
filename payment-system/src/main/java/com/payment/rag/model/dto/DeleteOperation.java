package com.payment.rag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 删除操作记录（用于补偿回滚）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteOperation {

    /**
     * 操作步骤
     */
    public enum Step {
        VECTOR_STORE_DELETE,  // Redis向量存储删除
        MYSQL_DELETE,         // MySQL数据库删除
        MINIO_DELETE         // MinIO文件删除
    }

    /**
     * 已完成的步骤
     */
    private List<Step> completedSteps = new ArrayList<>();

    /**
     * 删除的向量ID列表（用于回滚）
     */
    private List<String> deletedVectorIds = new ArrayList<>();

    /**
     * 删除的MySQL记录ID列表（用于回滚）
     */
    private List<String> deletedMySqlIds = new ArrayList<>();

    /**
     * 删除的MinIO路径
     */
    private String deletedMinioPath;

    /**
     * 记录完成的步骤
     */
    public void markCompleted(Step step) {
        completedSteps.add(step);
    }

    /**
     * 检查步骤是否已完成
     */
    public boolean isCompleted(Step step) {
        return completedSteps.contains(step);
    }
}
