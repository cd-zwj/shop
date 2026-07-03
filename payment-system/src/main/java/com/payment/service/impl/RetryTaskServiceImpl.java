package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.entity.RetryTask;
import com.payment.mapper.RetryTaskMapper;
import com.payment.service.RetryTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 重试任务管理服务实现类。
 * <p>提供重试任务的分页查询、手动重试（将失败/死亡/已取消任务重置为 PENDING）、
 * 手动取消待处理任务等运维管理功能。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryTaskServiceImpl implements RetryTaskService {

    private static final Set<String> RETRYABLE_STATUSES = Set.of("FAIL", "DEAD", "CANCELLED");
    private static final Set<String> CANCELLABLE_STATUSES = Set.of("PENDING");

    private final RetryTaskMapper retryTaskMapper;

    /**
     * 分页查询重试任务列表，支持按任务状态和任务类型过滤。
     *
     * @param taskStatus 任务状态筛选条件，为空则不过滤
     * @param taskType   任务类型筛选条件，为空则不过滤
     * @param current    当前页码
     * @param size       每页条数
     * @return 分页结果
     */
    @Override
    public Page<RetryTask> list(String taskStatus, String taskType, Integer current, Integer size) {
        LambdaQueryWrapper<RetryTask> wrapper = new LambdaQueryWrapper<>();
        if (taskStatus != null && !taskStatus.isBlank()) {
            wrapper.eq(RetryTask::getTaskStatus, taskStatus.toUpperCase());
        }
        if (taskType != null && !taskType.isBlank()) {
            wrapper.eq(RetryTask::getTaskType, taskType.toUpperCase());
        }
        wrapper.orderByDesc(RetryTask::getCreateTime);
        return retryTaskMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 手动重试指定任务，将失败/死亡/已取消的任务重置为待处理状态。
     * 重置后重试次数清零，下次重试时间设为当前时间。
     *
     * @param taskId 任务ID
     * @throws BusinessException 任务不存在或状态不可重试时抛出
     */
    @Override
    public void retry(Long taskId) {
        RetryTask task = retryTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("重试任务不存在");
        }
        if (!RETRYABLE_STATUSES.contains(task.getTaskStatus())) {
            throw new BusinessException("只有失败、死亡或已取消的任务才能重试");
        }
        task.setTaskStatus("PENDING");
        task.setRetryCount(0);
        task.setNextRetryTime(LocalDateTime.now());
        task.setLastErrorMessage("管理员手动重试");
        task.setUpdateTime(LocalDateTime.now());
        retryTaskMapper.updateById(task);
        log.info("重试任务已重置为 PENDING: taskId={}, bizNo={}", taskId, task.getBizNo());
    }

    /**
     * 手动取消指定的待处理任务。
     *
     * @param taskId 任务ID
     * @throws BusinessException 任务不存在或状态不可取消时抛出
     */
    @Override
    public void cancel(Long taskId) {
        RetryTask task = retryTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("重试任务不存在");
        }
        if (!CANCELLABLE_STATUSES.contains(task.getTaskStatus())) {
            throw new BusinessException("只有待处理的任务才能取消");
        }
        task.setTaskStatus("CANCELLED");
        task.setLastErrorMessage("管理员手动取消");
        task.setUpdateTime(LocalDateTime.now());
        retryTaskMapper.updateById(task);
        log.info("重试任务已取消: taskId={}, bizNo={}", taskId, task.getBizNo());
    }
}
