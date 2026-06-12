package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.RetryTask;

/**
 * 重试任务管理服务（管理端运维用）。
 */
public interface RetryTaskService {

    /** 分页查询重试任务列表，支持按状态和任务类型筛选。 */
    Page<RetryTask> list(String taskStatus, String taskType, Integer current, Integer size);

    /** 重试失败/死亡的重试任务：重置状态为 PENDING，清除下次重试时间。 */
    void retry(Long taskId);

    /** 取消待处理的重试任务。 */
    void cancel(Long taskId);
}
