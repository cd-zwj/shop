package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户优惠券表数据访问接口，提供用户持有优惠券的增删改查操作。
 * <p>对应实体表：{@link com.payment.entity.UserCoupon}</p>
 * <p>包含原子抢占优惠券库存的自定义方法，用于高并发领券场景。</p>
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 原子抢占优惠券库存：校验库存余量和每人限领，成功则 received_quantity +1。
     * @return 影响行数，0 表示库存不足或超限。
     */
    @Update("UPDATE coupon_template SET received_quantity = received_quantity + 1, update_time = NOW() "
            + "WHERE id = #{templateId} AND deleted = 0 "
            + "AND (total_quantity <= 0 OR received_quantity < total_quantity) "
            + "AND (per_user_limit <= 0 OR (SELECT COUNT(*) FROM user_coupon "
            + "    WHERE template_id = #{templateId} AND platform_user_id = #{platformUserId}) < per_user_limit)")
    int claimCouponSlot(@Param("templateId") Long templateId, @Param("platformUserId") Long platformUserId);
}
