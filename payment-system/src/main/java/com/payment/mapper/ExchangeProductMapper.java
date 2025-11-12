package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.ExchangeProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分兑换商品Mapper
 */
@Mapper
public interface ExchangeProductMapper extends BaseMapper<ExchangeProduct> {
}
