package com.payment.service;

import com.payment.dto.UserShippingAddressDTO;
import com.payment.entity.UserShippingAddress;

import java.util.List;

/**
 * 用户收货地址服务接口，用于定义地址管理业务能力。
 */
public interface UserShippingAddressService {

    /**
     * 查询当前用户地址列表。
     */
    List<UserShippingAddress> list(Long platformUserId);

    /**
     * 创建当前用户地址。
     */
    UserShippingAddress create(Long platformUserId, UserShippingAddressDTO dto);

    /**
     * 更新当前用户地址。
     */
    UserShippingAddress update(Long platformUserId, Long addressId, UserShippingAddressDTO dto);

    /**
     * 删除当前用户地址。
     */
    void delete(Long platformUserId, Long addressId);

    /**
     * 设置当前用户默认地址。
     */
    UserShippingAddress setDefault(Long platformUserId, Long addressId);
}
