package com.payment.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.OrderDeliveryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 订单配送记录数据访问接口，提供订单配送记录表（order_delivery_record）的 CRUD 操作。
 */
@Mapper
public interface OrderDeliveryRecordMapper extends BaseMapper<OrderDeliveryRecord> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM order_delivery_record
            WHERE id > #{cursor}
              AND payload IS NOT NULL
            ORDER BY id ASC
            LIMIT #{batchSize}
            """)
    List<OrderDeliveryRecord> selectPickupCodeRotationBatch(@Param("cursor") long cursor,
                                                            @Param("batchSize") int batchSize);

    @InterceptorIgnore(tenantLine = "true")
    @Update("""
            UPDATE order_delivery_record
            SET payload = #{newPayload}, update_time = NOW()
            WHERE id = #{id}
              AND payload = #{oldPayload}
            """)
    int compareAndSetPickupCodePayload(@Param("id") Long id,
                                       @Param("oldPayload") String oldPayload,
                                       @Param("newPayload") String newPayload);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT order_no)
            FROM order_delivery_record
            WHERE tenant_id = #{tenantId}
              AND deleted = 0
              AND status IN
              <foreach collection="statuses" item="status" open="(" separator="," close=")">
                #{status}
              </foreach>
            </script>
            """)
    Long countDistinctOrdersByTenantAndStatuses(@Param("tenantId") Long tenantId,
                                                @Param("statuses") List<String> statuses);
}
