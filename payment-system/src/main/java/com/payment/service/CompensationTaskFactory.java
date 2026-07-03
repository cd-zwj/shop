package com.payment.service;

import com.payment.entity.CompensationTask;

/**
 * 补偿任务工厂接口。
 * <p>
 * 负责创建补偿任务实例，采用幂等语义——若相同业务标识的补偿任务已存在则直接返回，
 * 避免重复创建。补偿任务用于处理异常场景下的数据最终一致性保障。
 */
public interface CompensationTaskFactory {

    /**
     * 创建补偿任务（幂等）。
     * <p>
     * 若相同 bizType + bizNo 的补偿任务已存在，直接返回已有任务，不会重复创建。
     *
     * @param bizType 业务类型（如 LATE_CALLBACK_REFUND 等）
     * @param bizNo   业务单号
     * @param remark  备注说明
     * @return 补偿任务实体（新建或已存在的）
     */
    CompensationTask createIfAbsent(String bizType, String bizNo, String remark);
}
