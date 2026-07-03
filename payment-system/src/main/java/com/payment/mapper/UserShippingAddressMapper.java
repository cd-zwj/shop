package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UserShippingAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户收货地址数据访问接口，提供用户收货地址表（user_shipping_address）的 CRUD 操作。
 * 支持多地址管理及默认地址切换。
 */
@Mapper
public interface UserShippingAddressMapper extends BaseMapper<UserShippingAddress> {

    /**
     * 清理指定平台用户的默认地址标记。
     */
    @Update("UPDATE user_shipping_address SET is_default = 0 WHERE platform_user_id = #{platformUserId} AND deleted = 0")
    int clearDefaultByPlatformUserId(@Param("platformUserId") Long platformUserId);
}
