package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.StoreInventoryChangeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店库存流水数据访问。
 */
@Mapper
public interface StoreInventoryChangeLogMapper extends BaseMapper<StoreInventoryChangeLog> {
}
