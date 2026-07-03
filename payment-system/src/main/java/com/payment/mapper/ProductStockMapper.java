package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.ProductStock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品库存数据访问接口，提供商品库存表（product_stock）的 CRUD 操作。
 */
@Mapper
public interface ProductStockMapper extends BaseMapper<ProductStock> {
}

