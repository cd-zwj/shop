package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售订单数据访问接口，提供销售订单表（sales_order）的 CRUD 操作。
 */
@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {
}
