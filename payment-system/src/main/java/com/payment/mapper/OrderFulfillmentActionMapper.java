package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.OrderFulfillmentAction;
import org.apache.ibatis.annotations.Mapper;

/** 门店自提履约操作流水数据访问。 */
@Mapper
public interface OrderFulfillmentActionMapper extends BaseMapper<OrderFulfillmentAction> {
}
