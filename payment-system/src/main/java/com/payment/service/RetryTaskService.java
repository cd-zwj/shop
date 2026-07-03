package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.RetryTask;

/**
 * 重试任务管理服务接口（管理端运维用）。
 * <p>
 * 提供重试任务的查询、重试和取消能力，供平台管理员在运维场景下
 * 对失败或死亡的重试任务进行人工干预。重试任务与补偿任务互补，
 * 用于需要定时重试的异步操作（如消息投递、索引同步等）。
 */
public interface RetryTaskService {

    /**
     * 分页查询重试任务列表，支持按状态和任务类型筛选。
     *
     * @param taskStatus 任务状态筛选（可选，如 PENDING、DEAD、SUCCESS 等）
     * @param taskType   任务类型筛选（可选）
     * @param current    当前页码
     * @param size       每页数量
     * @return 重试任务分页结果
     */
    Page<RetryTask> list(String taskStatus, String taskType, Integer current, Integer size);

    /**
     * 重试失败/死亡的重试任务：重置状态为 PENDING，清除下次重试时间。
     *
     * @param taskId 重试任务 ID
     */
    void retry(Long taskId);

    /**
     * 取消待处理的重试任务。
     *
     * @param taskId 重试任务 ID
     */
    void cancel(Long taskId);
}
