package com.payment.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商品数据访问接口，提供商品表（product）的增删改查操作。
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT id, tenant_id, product_code, name, price, unit, category, image_url, description,
                   store_id, virtual_type_id, virtual_category_id, fulfillment_mode,
                   product_type, delivery_config, status, deleted, create_time, update_time
            FROM product
            WHERE id = #{productId}
              AND status = 1
              AND deleted = 0
            """)
    Product selectVisibleAppProductById(@Param("productId") Long productId);
}
