package com.payment.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.RetryTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 重试任务 Mapper 接口
 * <p>
 * 管理消息重试任务的持久化操作。
 * 当消息首次消费失败后，系统会创建重试任务记录，
 * 按照指数退避策略进行多次重试，直至成功或转入死信队列。
 * </p>
 *
 * @author payment-system
 */
@Mapper
public interface RetryTaskMapper extends BaseMapper<RetryTask> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT COUNT(1)
            FROM retry_task rt
            WHERE rt.task_status IN ('PENDING', 'PROCESSING', 'FAIL', 'DEAD')
              AND (
                    EXISTS (
                        SELECT 1
                        FROM sales_order so
                        WHERE so.tenant_id = #{tenantId}
                          AND so.deleted = 0
                          AND so.order_no = rt.biz_no
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM payment_bill pb
                        WHERE pb.tenant_id = #{tenantId}
                          AND pb.bill_no = rt.biz_no
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM refund_order ro
                        WHERE ro.tenant_id = #{tenantId}
                          AND ro.deleted = 0
                          AND (ro.refund_no = rt.biz_no OR ro.payment_bill_no = rt.biz_no)
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM refund_application ra
                        WHERE ra.tenant_id = #{tenantId}
                          AND ra.refund_no = rt.biz_no
                    )
                    OR JSON_UNQUOTE(JSON_EXTRACT(rt.extension_json, '$.tenantId')) = CAST(#{tenantId} AS CHAR)
                  )
            """)
    Long countMerchantVisibleOpenTasks(@Param("tenantId") Long tenantId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT rt.*
            FROM retry_task rt
            WHERE rt.task_status IN ('PENDING', 'PROCESSING', 'FAIL', 'DEAD')
              AND (
                    EXISTS (
                        SELECT 1
                        FROM sales_order so
                        WHERE so.tenant_id = #{tenantId}
                          AND so.deleted = 0
                          AND so.order_no = rt.biz_no
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM payment_bill pb
                        WHERE pb.tenant_id = #{tenantId}
                          AND pb.bill_no = rt.biz_no
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM refund_order ro
                        WHERE ro.tenant_id = #{tenantId}
                          AND ro.deleted = 0
                          AND (ro.refund_no = rt.biz_no OR ro.payment_bill_no = rt.biz_no)
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM refund_application ra
                        WHERE ra.tenant_id = #{tenantId}
                          AND ra.refund_no = rt.biz_no
                    )
                    OR JSON_UNQUOTE(JSON_EXTRACT(rt.extension_json, '$.tenantId')) = CAST(#{tenantId} AS CHAR)
                  )
            ORDER BY rt.update_time DESC, rt.id DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<RetryTask> selectMerchantVisibleOpenTasks(@Param("tenantId") Long tenantId,
                                                   @Param("size") Integer size,
                                                   @Param("offset") Integer offset);
}
