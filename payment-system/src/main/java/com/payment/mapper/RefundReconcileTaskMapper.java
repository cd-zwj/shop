package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.RefundReconcileTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款对账任务表数据访问接口，提供退款对账任务的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.RefundReconcileTask}</p>
 */
@Mapper
public interface RefundReconcileTaskMapper extends BaseMapper<RefundReconcileTask> {
}
