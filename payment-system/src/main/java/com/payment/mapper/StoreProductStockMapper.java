package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.StoreProductStock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 门店库存数据访问。
 */
@Mapper
public interface StoreProductStockMapper extends BaseMapper<StoreProductStock> {

    /** 原子确保库存行存在；重复键分支同时取得该唯一键的排他锁。 */
    @Insert("""
            INSERT INTO store_product_stock
                (tenant_id, store_id, product_id, quantity, locked_quantity, version)
            VALUES (#{tenantId}, #{storeId}, #{productId}, 0, 0, 0)
            ON DUPLICATE KEY UPDATE id = id
            """)
    int ensureExists(@Param("tenantId") Long tenantId,
                     @Param("storeId") Long storeId,
                     @Param("productId") Long productId);

    /**
     * 在事务内串行化同一门店商品的库存变更。
     */
    @Select("""
            SELECT id, tenant_id, store_id, product_id, quantity, locked_quantity, version, create_time, update_time
            FROM store_product_stock
            WHERE tenant_id = #{tenantId}
              AND store_id = #{storeId}
              AND product_id = #{productId}
            FOR UPDATE
            """)
    StoreProductStock selectForUpdate(@Param("tenantId") Long tenantId,
                                      @Param("storeId") Long storeId,
                                      @Param("productId") Long productId);
}
