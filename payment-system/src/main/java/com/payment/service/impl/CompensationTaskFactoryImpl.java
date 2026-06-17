package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.entity.CompensationTask;
import com.payment.mapper.CompensationTaskMapper;
import com.payment.service.CompensationTaskFactory;
import com.payment.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompensationTaskFactoryImpl implements CompensationTaskFactory {

    private final CompensationTaskMapper compensationTaskMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompensationTask createIfAbsent(String bizType, String bizNo, String remark) {
        String normalizedBizType = normalizeBizType(bizType);
        if (bizNo == null || bizNo.isBlank()) {
            throw new BusinessException("补偿任务业务单号不能为空");
        }
        CompensationTask existing = compensationTaskMapper.selectOne(new LambdaQueryWrapper<CompensationTask>()
                .eq(CompensationTask::getBizType, normalizedBizType)
                .eq(CompensationTask::getBizNo, bizNo));
        if (existing != null) {
            return existing;
        }

        CompensationTask task = new CompensationTask();
        task.setTaskNo(BizNoGenerator.generate("CT"));
        task.setBizType(normalizedBizType);
        task.setBizNo(bizNo);
        task.setTaskStatus("PENDING");
        task.setRemark(remark);
        task.setRetryCount(0);
        compensationTaskMapper.insert(task);
        return task;
    }

    private String normalizeBizType(String bizType) {
        if (bizType == null || bizType.isBlank()) {
            throw new BusinessException("补偿任务业务类型不能为空");
        }
        return bizType.toUpperCase();
    }
}
