package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.OrderDiscountSnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单优惠快照数据访问接口。
 */
@Mapper
public interface OrderDiscountSnapshotMapper extends BaseMapper<OrderDiscountSnapshot> {
}
