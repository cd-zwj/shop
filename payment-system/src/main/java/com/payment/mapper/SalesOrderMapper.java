package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 销售订单数据访问接口，提供销售订单表（sales_order）的 CRUD 操作。
 */
@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {

    @Select("""
            SELECT COUNT(1)
            FROM sales_order so
            WHERE so.tenant_id = #{tenantId}
              AND so.deleted = 0
              AND (
                    so.pay_status = 'FAILED'
                    OR EXISTS (
                        SELECT 1
                        FROM order_delivery_record odr
                        WHERE odr.tenant_id = so.tenant_id
                          AND odr.order_no = so.order_no
                          AND odr.deleted = 0
                          AND odr.status IN ('FAILED', 'REVOKE_FAILED')
                    )
                  )
            """)
    Long countAbnormalOrdersByTenant(@Param("tenantId") Long tenantId);
}
