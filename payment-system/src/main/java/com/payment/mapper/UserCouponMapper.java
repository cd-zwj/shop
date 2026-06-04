package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户优惠券数据访问接口。
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 原子抢占优惠券库存：校验库存余量和每人限领，成功则 received_count +1。
     * @return 影响行数，0 表示库存不足或超限。
     */
    @Update("UPDATE coupon_template SET received_count = received_count + 1, update_time = NOW() "
            + "WHERE id = #{templateId} AND deleted = 0 "
            + "AND (total_stock <= 0 OR received_count < total_stock) "
            + "AND (per_user_limit <= 0 OR (SELECT COUNT(*) FROM user_coupon "
            + "    WHERE coupon_template_id = #{templateId} AND platform_user_id = #{platformUserId} AND deleted = 0) < per_user_limit)")
    int claimCouponSlot(@Param("templateId") Long templateId, @Param("platformUserId") Long platformUserId);
}
