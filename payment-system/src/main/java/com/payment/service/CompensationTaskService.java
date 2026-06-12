package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.CompensationTask;

/**
 * 补偿任务管理服务（管理端运维用）。
 */
public interface CompensationTaskService {

    /** 分页查询补偿任务列表，支持按状态和业务类型筛选。 */
    Page<CompensationTask> list(String taskStatus, String bizType, Integer current, Integer size);

    /** 重试失败的补偿任务：重置状态为 PENDING。 */
    void retry(Long taskId);

    /** 取消待处理的补偿任务。 */
    void cancel(Long taskId);
}
