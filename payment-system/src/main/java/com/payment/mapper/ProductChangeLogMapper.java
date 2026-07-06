package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.ProductChangeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品价格/库存变更流水 Mapper。
 */
@Mapper
public interface ProductChangeLogMapper extends BaseMapper<ProductChangeLog> {
}
