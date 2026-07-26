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

    @Select("""
            SELECT COUNT(1)
            FROM store_product_stock stock
            JOIN store_product relation
              ON relation.tenant_id = stock.tenant_id
             AND relation.store_id = stock.store_id
             AND relation.product_id = stock.product_id
            JOIN product product
              ON product.id = stock.product_id
             AND product.tenant_id = stock.tenant_id
            WHERE stock.tenant_id = #{tenantId}
              AND relation.status = 1
              AND product.status = 1
              AND product.deleted = 0
              AND stock.quantity - stock.locked_quantity <= #{threshold}
            """)
    Long countActiveLowStockByTenant(@Param("tenantId") Long tenantId,
                                     @Param("threshold") Integer threshold);
}
