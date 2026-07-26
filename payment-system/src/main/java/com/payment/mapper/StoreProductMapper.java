package com.payment.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.Product;
import com.payment.entity.StoreProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StoreProductMapper extends BaseMapper<StoreProduct> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT p.id, p.tenant_id, p.product_code, p.name,
                   COALESCE(sp.price, p.price) AS price, p.unit, p.category, p.image_url, p.description,
                   sp.store_id, 'STORE_PICKUP' AS fulfillment_mode, p.status, p.deleted, p.create_time, p.update_time,
                   GREATEST(COALESCE(stock.quantity, 0) - COALESCE(stock.locked_quantity, 0), 0) AS stock
            FROM store_product sp
            JOIN product p ON p.id = sp.product_id AND p.tenant_id = sp.tenant_id
            JOIN store s ON s.id = sp.store_id AND s.tenant_id = sp.tenant_id
            LEFT JOIN store_product_stock stock
                   ON stock.tenant_id = sp.tenant_id
                  AND stock.store_id = sp.store_id
                  AND stock.product_id = sp.product_id
            WHERE sp.tenant_id = #{tenantId}
              AND sp.store_id = #{storeId}
              AND sp.status = 1
              AND s.status = 1 AND s.deleted = 0
              AND p.status = 1 AND p.deleted = 0
              AND COALESCE(stock.quantity, 0) > COALESCE(stock.locked_quantity, 0)
            ORDER BY p.create_time DESC
            """)
    List<Product> selectVisibleProductsByStore(@Param("tenantId") Long tenantId, @Param("storeId") Long storeId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT p.id, p.tenant_id, p.product_code, p.name,
                   COALESCE(sp.price, p.price) AS price, p.unit, p.category, p.image_url, p.description,
                   sp.store_id, 'STORE_PICKUP' AS fulfillment_mode, p.status, p.deleted, p.create_time, p.update_time,
                   GREATEST(COALESCE(stock.quantity, 0) - COALESCE(stock.locked_quantity, 0), 0) AS stock
            FROM store_product sp
            JOIN product p ON p.id = sp.product_id AND p.tenant_id = sp.tenant_id
            JOIN store s ON s.id = sp.store_id AND s.tenant_id = sp.tenant_id
            LEFT JOIN store_product_stock stock
                   ON stock.tenant_id = sp.tenant_id
                  AND stock.store_id = sp.store_id
                  AND stock.product_id = sp.product_id
            WHERE (#{tenantId} IS NULL OR sp.tenant_id = #{tenantId})
              AND p.id = #{productId}
              AND sp.store_id = #{storeId}
              AND sp.status = 1
              AND s.status = 1 AND s.deleted = 0
              AND p.status = 1 AND p.deleted = 0
            """)
    Product selectVisibleProductByStore(@Param("tenantId") Long tenantId,
                                        @Param("productId") Long productId,
                                        @Param("storeId") Long storeId);
}
