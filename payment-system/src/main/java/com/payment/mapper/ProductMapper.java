package com.payment.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.Product;
import com.payment.entity.ProductStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

/**
 * 商品数据访问接口，提供商品表（product）的增删改查操作。
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            <script>
            SELECT product_id AS productId, tenant_id AS tenantId, quantity
            FROM product_stock
            WHERE tenant_id = #{tenantId}
              AND product_id IN
              <foreach collection="productIds" item="productId" open="(" separator="," close=")">
                #{productId}
              </foreach>
            </script>
            """)
    List<ProductStock> selectStockByTenantAndProductIds(@Param("tenantId") Long tenantId,
                                                        @Param("productIds") Set<Long> productIds);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT p.id, p.tenant_id, p.product_code, p.name, p.price, p.unit, p.category, p.image_url,
                   p.description, p.store_id, p.virtual_type_id, p.virtual_category_id, p.fulfillment_mode,
                   p.product_type, p.delivery_config, p.status, p.deleted, p.create_time, p.update_time,
                   COALESCE(ps.quantity, 0) AS stock
            FROM product p
            LEFT JOIN product_stock ps
              ON ps.product_id = p.id
             AND ps.tenant_id = p.tenant_id
            WHERE p.id = #{productId}
              AND p.status = 1
              AND p.deleted = 0
            """)
    Product selectVisibleAppProductById(@Param("productId") Long productId);
}
