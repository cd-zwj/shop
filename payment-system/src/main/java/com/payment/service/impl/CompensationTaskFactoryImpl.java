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

/**
 * 补偿任务工厂实现类。
 * <p>
 * 负责幂等创建补偿任务，保证同一业务类型+业务单号仅产生一条补偿任务记录。
 * 使用 {@link Propagation#REQUIRES_NEW} 独立事务，确保补偿任务的创建不受外层业务事务回滚影响。
 * <p>
 * 适用场景：支付超时回调、退款延迟、消息投递失败等需要异步补偿重试的业务。
 *
 * @see com.payment.service.CompensationTaskFactory
 */
@Service
@RequiredArgsConstructor
public class CompensationTaskFactoryImpl implements CompensationTaskFactory {

    private final CompensationTaskMapper compensationTaskMapper;

    /**
     * 幂等创建补偿任务。
     * <p>
     * 若相同 bizType + bizNo 的任务已存在，则直接返回已有任务，不重复创建。
     * 新建任务初始状态为 PENDING，重试次数为 0。
     *
     * @param bizType 业务类型（自动转大写），不能为空
     * @param bizNo   业务单号，不能为空
     * @param remark  任务备注说明
     * @return 已存在或新创建的补偿任务实体
     * @throws BusinessException 业务类型或业务单号为空时抛出
     */
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

    /**
     * 校验并规范化业务类型（转大写）。
     *
     * @param bizType 原始业务类型
     * @return 大写后的业务类型
     * @throws BusinessException 业务类型为空时抛出
     */
    private String normalizeBizType(String bizType) {
        if (bizType == null || bizType.isBlank()) {
            throw new BusinessException("补偿任务业务类型不能为空");
        }
        return bizType.toUpperCase();
    }
}
