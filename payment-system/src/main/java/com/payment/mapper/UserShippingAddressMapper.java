package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserShippingAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户收货地址数据访问接口，用于执行地址数据的增删改查。
 */
@Mapper
public interface UserShippingAddressMapper extends BaseMapper<UserShippingAddress> {

    /**
     * 清理指定平台用户的默认地址标记。
     */
    @Update("UPDATE user_shipping_address SET is_default = 0 WHERE platform_user_id = #{platformUserId} AND deleted = 0")
    int clearDefaultByPlatformUserId(@Param("platformUserId") Long platformUserId);
}
