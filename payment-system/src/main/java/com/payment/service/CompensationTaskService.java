package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.CompensationTask;

/**
 * 补偿任务管理服务接口（管理端运维用）。
 * <p>
 * 提供补偿任务的查询、重试和取消能力，供平台管理员在运维场景下
 * 对异常未完成的补偿任务进行人工干预。
 */
public interface CompensationTaskService {

    /**
     * 分页查询补偿任务列表，支持按状态和业务类型筛选。
     *
     * @param taskStatus 任务状态筛选（可选，如 PENDING、SUCCESS、FAILED 等）
     * @param bizType    业务类型筛选（可选）
     * @param current    当前页码
     * @param size       每页数量
     * @return 补偿任务分页结果
     */
    Page<CompensationTask> list(String taskStatus, String bizType, Integer current, Integer size);

    /**
     * 重试失败的补偿任务：重置状态为 PENDING。
     *
     * @param taskId 补偿任务 ID
     */
    void retry(Long taskId);

    /**
     * 取消待处理的补偿任务。
     *
     * @param taskId 补偿任务 ID
     */
    void cancel(Long taskId);
}
