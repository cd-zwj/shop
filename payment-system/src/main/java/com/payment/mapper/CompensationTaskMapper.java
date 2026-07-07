package com.payment.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.CompensationTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 补偿任务 Mapper 接口
 * <p>
 * 管理消息补偿任务的持久化操作。
 * 当消息处理失败需要补偿时，系统会创建补偿任务记录，
 * 由定时任务或手动触发进行补偿执行。
 * </p>
 *
 * @author payment-system
 */
@Mapper
public interface CompensationTaskMapper extends BaseMapper<CompensationTask> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT COUNT(1)
            FROM compensation_task ct
            WHERE ct.task_status IN ('PENDING', 'PROCESSING', 'FAIL')
              AND (
                    EXISTS (
                        SELECT 1
                        FROM sales_order so
                        WHERE so.tenant_id = #{tenantId}
                          AND so.deleted = 0
                          AND so.order_no = ct.biz_no
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM payment_bill pb
                        WHERE pb.tenant_id = #{tenantId}
                          AND pb.bill_no = ct.biz_no
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM refund_order ro
                        WHERE ro.tenant_id = #{tenantId}
                          AND ro.deleted = 0
                          AND (ro.refund_no = ct.biz_no OR ro.payment_bill_no = ct.biz_no)
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM refund_application ra
                        WHERE ra.tenant_id = #{tenantId}
                          AND ra.refund_no = ct.biz_no
                    )
                  )
            """)
    Long countMerchantVisibleOpenTasks(@Param("tenantId") Long tenantId);
}
