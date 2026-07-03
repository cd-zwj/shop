package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.OrderDiscountSnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单优惠快照数据访问接口，提供订单优惠快照表（order_discount_snapshot）的 CRUD 操作。
 * 记录下单时享受的优惠详情，用于售后退款核对。
 */
@Mapper
public interface OrderDiscountSnapshotMapper extends BaseMapper<OrderDiscountSnapshot> {
}
