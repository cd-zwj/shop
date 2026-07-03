package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CouponOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券操作日志表数据访问接口，提供优惠券全生命周期操作日志的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.CouponOperationLog}</p>
 */
@Mapper
public interface CouponOperationLogMapper extends BaseMapper<CouponOperationLog> {
}
