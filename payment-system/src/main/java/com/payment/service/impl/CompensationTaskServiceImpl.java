package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.entity.CompensationTask;
import com.payment.mapper.CompensationTaskMapper;
import com.payment.service.CompensationTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 补偿任务服务实现类。
 * <p>
 * 提供补偿任务的查询、重试和取消管理功能，供平台管理员在后台对异步补偿任务进行运维操作。
 * <ul>
 *   <li><b>查询</b>：支持按任务状态、业务类型分页筛选</li>
 *   <li><b>重试</b>：仅允许将失败或已取消的任务重置为 PENDING 状态</li>
 *   <li><b>取消</b>：仅允许取消待处理状态的任务</li>
 * </ul>
 *
 * @see com.payment.service.CompensationTaskService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompensationTaskServiceImpl implements CompensationTaskService {

    private final CompensationTaskMapper compensationTaskMapper;

    /**
     * 分页查询补偿任务列表。
     *
     * @param taskStatus 任务状态筛选条件（PENDING / PROCESSING / SUCCESS / FAIL / CANCELLED），可为 null
     * @param bizType    业务类型筛选条件，可为 null
     * @param current    当前页码
     * @param size       每页条数
     * @return 分页结果，按创建时间降序排列
     */
    @Override
    public Page<CompensationTask> list(String taskStatus, String bizType, Integer current, Integer size) {
        LambdaQueryWrapper<CompensationTask> wrapper = new LambdaQueryWrapper<>();
        if (taskStatus != null && !taskStatus.isBlank()) {
            wrapper.eq(CompensationTask::getTaskStatus, taskStatus.toUpperCase());
        }
        if (bizType != null && !bizType.isBlank()) {
            wrapper.eq(CompensationTask::getBizType, bizType.toUpperCase());
        }
        wrapper.orderByDesc(CompensationTask::getCreateTime);
        return compensationTaskMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 重试补偿任务。
     * <p>
     * 仅允许将失败（FAIL）或已取消（CANCELLED）的任务重置为 PENDING 状态，
     * 重试次数归零，由调度器重新拾取执行。
     *
     * @param taskId 任务 ID
     * @throws BusinessException 任务不存在或状态不允许重试时抛出
     */
    @Override
    public void retry(Long taskId) {
        CompensationTask task = compensationTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("补偿任务不存在");
        }
        if (!"FAIL".equals(task.getTaskStatus()) && !"CANCELLED".equals(task.getTaskStatus())) {
            throw new BusinessException("只有失败或已取消的任务才能重试");
        }
        task.setTaskStatus("PENDING");
        task.setRetryCount(0);
        task.setRemark("管理员手动重试");
        task.setUpdateTime(LocalDateTime.now());
        compensationTaskMapper.updateById(task);
        log.info("补偿任务已重置为 PENDING: taskId={}, bizNo={}", taskId, task.getBizNo());
    }

    /**
     * 取消补偿任务。
     * <p>
     * 仅允许取消待处理（PENDING）状态的任务，将状态置为 CANCELLED。
     *
     * @param taskId 任务 ID
     * @throws BusinessException 任务不存在或状态不允许取消时抛出
     */
    @Override
    public void cancel(Long taskId) {
        CompensationTask task = compensationTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("补偿任务不存在");
        }
        if (!"PENDING".equals(task.getTaskStatus())) {
            throw new BusinessException("只有待处理的任务才能取消");
        }
        task.setTaskStatus("CANCELLED");
        task.setRemark("管理员手动取消");
        task.setUpdateTime(LocalDateTime.now());
        compensationTaskMapper.updateById(task);
        log.info("补偿任务已取消: taskId={}, bizNo={}", taskId, task.getBizNo());
    }
}
