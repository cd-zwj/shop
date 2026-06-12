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

@Slf4j
@Service
@RequiredArgsConstructor
public class CompensationTaskServiceImpl implements CompensationTaskService {

    private final CompensationTaskMapper compensationTaskMapper;

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
