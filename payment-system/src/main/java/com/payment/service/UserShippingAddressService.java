package com.payment.service;

import com.payment.dto.UserShippingAddressDTO;
import com.payment.entity.UserShippingAddress;

import java.util.List;

/**
 * 用户收货地址服务接口。
 *
 * <p>面向 C 端用户提供收货地址的增删改查和默认地址设置能力，
 * 每个用户最多维护一定数量的收货地址。承接 {@code V1AppAddressController} 的业务逻辑。</p>
 */
public interface UserShippingAddressService {

    /**
     * 查询当前用户的全部收货地址列表。
     *
     * @param platformUserId 平台用户ID
     * @return 收货地址列表（默认地址排在首位）
     */
    List<UserShippingAddress> list(Long platformUserId);

    /**
     * 创建收货地址。
     *
     * <p>若用户尚无地址则自动设为默认地址。</p>
     *
     * @param platformUserId 平台用户ID
     * @param dto            地址请求 DTO（含收件人、手机号、省市区、详细地址等）
     * @return 创建成功的地址实体
     */
    UserShippingAddress create(Long platformUserId, UserShippingAddressDTO dto);

    /**
     * 更新收货地址。
     *
     * @param platformUserId 平台用户ID
     * @param addressId      地址ID
     * @param dto            地址请求 DTO
     * @return 更新后的地址实体
     * @throws com.payment.common.exception.BusinessException 地址不存在或不属于当前用户时抛出
     */
    UserShippingAddress update(Long platformUserId, Long addressId, UserShippingAddressDTO dto);

    /**
     * 删除收货地址。
     *
     * <p>若删除的是默认地址，系统将自动将最新一条地址设为默认。</p>
     *
     * @param platformUserId 平台用户ID
     * @param addressId      地址ID
     * @throws com.payment.common.exception.BusinessException 地址不存在或不属于当前用户时抛出
     */
    void delete(Long platformUserId, Long addressId);

    /**
     * 设置默认收货地址。
     *
     * <p>将指定地址设为默认，同时取消原默认地址的默认标记。</p>
     *
     * @param platformUserId 平台用户ID
     * @param addressId      地址ID
     * @return 更新后的地址实体
     * @throws com.payment.common.exception.BusinessException 地址不存在或不属于当前用户时抛出
     */
    UserShippingAddress setDefault(Long platformUserId, Long addressId);
}
