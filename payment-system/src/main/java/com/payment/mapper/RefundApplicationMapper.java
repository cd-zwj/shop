package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.RefundApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;

/**
 * 退款申请表数据访问接口，提供退款申请记录的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.RefundApplication}</p>
 */
@Mapper
public interface RefundApplicationMapper extends BaseMapper<RefundApplication> {

    @Select("""
            <script>
            SELECT ra.*
            FROM refund_application ra
            INNER JOIN sales_order so
              ON so.tenant_id = ra.tenant_id
             AND so.order_no = ra.order_no
             AND so.deleted = 0
            WHERE ra.tenant_id = #{tenantId}
            <if test="status != null and status != ''">
              AND ra.refund_status = #{status}
            </if>
            <if test="storeIds != null and storeIds.size() > 0">
              AND so.store_id IN
              <foreach collection="storeIds" item="storeId" open="(" close=")" separator=",">
                #{storeId}
              </foreach>
            </if>
            <if test="storeIds != null and storeIds.size() == 0">
              AND 1 = 0
            </if>
            ORDER BY ra.create_time DESC
            </script>
            """)
    Page<RefundApplication> selectMerchantPage(Page<RefundApplication> page,
                                               @Param("tenantId") Long tenantId,
                                               @Param("status") String status,
                                               @Param("storeIds") List<Long> storeIds);

    @Select("""
            <script>
            SELECT ra.*
            FROM refund_application ra
            WHERE 1 = 1
            <if test="tenantId != null">
              AND ra.tenant_id = #{tenantId}
            </if>
            <if test="status != null and status != ''">
              AND ra.refund_status = #{status}
            </if>
            <if test="keyword != null and keyword != ''">
              AND (ra.refund_no LIKE CONCAT('%', #{keyword}, '%')
                   OR ra.order_no LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY ra.create_time DESC, ra.id DESC
            </script>
            """)
    Page<RefundApplication> selectAdminPage(Page<RefundApplication> page,
                                            @Param("tenantId") Long tenantId,
                                            @Param("status") String status,
                                            @Param("keyword") String keyword);

    @Update("""
            UPDATE refund_application
            SET refund_status = #{targetStatus},
                admin_id = #{adminId},
                audit_time = NOW(),
                reject_reason = #{rejectReason}
            WHERE id = #{refundId}
              AND tenant_id = #{tenantId}
              AND refund_status = #{expectedStatus}
            """)
    int claimDecision(@Param("refundId") Long refundId,
                      @Param("tenantId") Long tenantId,
                      @Param("expectedStatus") String expectedStatus,
                      @Param("targetStatus") String targetStatus,
                      @Param("adminId") Long adminId,
                      @Param("rejectReason") String rejectReason);

    @Update("""
            UPDATE refund_application
            SET refund_status = 'CANCELLED'
            WHERE id = #{refundId}
              AND tenant_id = #{tenantId}
              AND platform_user_id = #{platformUserId}
              AND refund_status = 'PENDING'
            """)
    int cancelPending(@Param("refundId") Long refundId,
                      @Param("tenantId") Long tenantId,
                      @Param("platformUserId") Long platformUserId);
}
