package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品分类数据访问接口，提供商品分类表（product_category）的 CRUD 操作。
 */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {
}
