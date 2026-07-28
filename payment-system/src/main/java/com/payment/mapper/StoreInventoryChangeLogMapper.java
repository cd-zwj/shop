package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.StoreInventoryChangeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 门店库存流水数据访问。
 */
@Mapper
public interface StoreInventoryChangeLogMapper extends BaseMapper<StoreInventoryChangeLog> {

    /** 库存行锁之后使用当前读检查幂等流水，避免并发重放命中旧快照。 */
    @Select("""
            SELECT id, tenant_id, store_id, product_id, change_type, change_quantity,
                   quantity_before, quantity_after, locked_before, locked_after,
                   biz_type, biz_no, operator_id, remark, create_time
            FROM store_inventory_change_log
            WHERE tenant_id = #{tenantId}
              AND store_id = #{storeId}
              AND product_id = #{productId}
              AND change_type = #{changeType}
              AND biz_type = #{bizType}
              AND biz_no = #{bizNo}
            FOR UPDATE
            """)
    StoreInventoryChangeLog selectBizRecordForUpdate(@Param("tenantId") Long tenantId,
                                                       @Param("storeId") Long storeId,
                                                       @Param("productId") Long productId,
                                                       @Param("changeType") String changeType,
                                                       @Param("bizType") String bizType,
                                                       @Param("bizNo") String bizNo);
}
