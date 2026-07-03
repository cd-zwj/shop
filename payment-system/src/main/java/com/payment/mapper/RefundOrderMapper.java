package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.RefundOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款订单表数据访问接口，提供退款订单记录的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.RefundOrder}</p>
 */
@Mapper
public interface RefundOrderMapper extends BaseMapper<RefundOrder> {
}
