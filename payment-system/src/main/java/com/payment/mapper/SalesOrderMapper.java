package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 销售订单数据访问接口，提供销售订单表（sales_order）的 CRUD 操作。
 */
@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {

    /**
     * 按订单号加行锁读取，作为“支付成功”与“超时关闭”的唯一并发裁决点。
     * 调用方必须处于事务中。
     */
    @Select("""
            SELECT * FROM sales_order
            WHERE order_no = #{orderNo} AND deleted = 0
            FOR UPDATE
            """)
    SalesOrder selectByOrderNoForUpdate(@Param("orderNo") String orderNo);

    /** 支付渠道确认成功后，抢占订单为资金已确认中间态。 */
    @Update("""
            UPDATE sales_order
            SET order_status = 'PAID', pay_status = 'SUCCESS', update_time = NOW()
            WHERE id = #{id} AND deleted = 0
              AND order_status = 'CREATED' AND pay_status = 'WAIT_PAY'
            """)
    int claimPayment(@Param("id") Long id);

    /** 明确支付失败后关闭订单；只有待支付订单可取得处理权。 */
    @Update("""
            UPDATE sales_order
            SET order_status = 'CLOSED', pay_status = 'FAILED', update_time = NOW()
            WHERE id = #{id} AND deleted = 0
              AND order_status = 'CREATED' AND pay_status = 'WAIT_PAY'
            """)
    int failPayment(@Param("id") Long id);

    /** 用户取消待支付订单时取得唯一资源释放权。 */
    @Update("""
            UPDATE sales_order
            SET order_status = 'CANCELLED', pay_status = 'CLOSED', update_time = NOW()
            WHERE id = #{id} AND deleted = 0
              AND order_status = 'CREATED' AND pay_status = 'WAIT_PAY'
            """)
    int cancelUnpaid(@Param("id") Long id);

    /** 支付后处理完成后，将受控中间态推进到待备货。 */
    @Update("""
            UPDATE sales_order
            SET order_status = 'PENDING_PREPARATION', update_time = NOW()
            WHERE id = #{id} AND deleted = 0
              AND order_status = 'PAID' AND pay_status = 'SUCCESS'
            """)
    int completePaymentProcessing(@Param("id") Long id);

    Page<SalesOrder> selectMerchantOrders(Page<SalesOrder> page,
                                          @Param("tenantId") Long tenantId,
                                          @Param("orderStatus") String orderStatus,
                                          @Param("payStatus") String payStatus,
                                          @Param("keyword") String keyword,
                                          @Param("fulfillmentStatus") String fulfillmentStatus,
                                          @Param("deliveryStatuses") List<String> deliveryStatuses);

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
