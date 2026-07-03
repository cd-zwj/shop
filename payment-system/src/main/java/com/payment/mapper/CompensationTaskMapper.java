package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CompensationTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 补偿任务 Mapper 接口
 * <p>
 * 管理消息补偿任务的持久化操作。
 * 当消息处理失败需要补偿时，系统会创建补偿任务记录，
 * 由定时任务或手动触发进行补偿执行。
 * </p>
 *
 * @author payment-system
 */
@Mapper
public interface CompensationTaskMapper extends BaseMapper<CompensationTask> {
}
