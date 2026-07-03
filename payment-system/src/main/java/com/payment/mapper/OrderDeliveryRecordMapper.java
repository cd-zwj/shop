package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.OrderDeliveryRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单配送记录数据访问接口，提供订单配送记录表（order_delivery_record）的 CRUD 操作。
 */
@Mapper
public interface OrderDeliveryRecordMapper extends BaseMapper<OrderDeliveryRecord> {
}
