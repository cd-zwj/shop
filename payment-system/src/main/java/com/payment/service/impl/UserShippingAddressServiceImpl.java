package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.dto.UserShippingAddressDTO;
import com.payment.entity.UserShippingAddress;
import com.payment.mapper.UserShippingAddressMapper;
import com.payment.service.UserShippingAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 用户收货地址服务实现类，用于处理地址增删改查和默认地址约束。
 */
@Service
@RequiredArgsConstructor
public class UserShippingAddressServiceImpl implements UserShippingAddressService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final UserShippingAddressMapper addressMapper;

    /**
     * 查询地址列表。
     */
    @Override
    public List<UserShippingAddress> list(Long platformUserId) {
        return addressMapper.selectList(baseUserWrapper(platformUserId)
                .orderByDesc(UserShippingAddress::getIsDefault)
                .orderByDesc(UserShippingAddress::getUpdateTime)
                .orderByDesc(UserShippingAddress::getId));
    }

    /**
     * 创建地址。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserShippingAddress create(Long platformUserId, UserShippingAddressDTO dto) {
        validateAddress(dto);
        long addressCount = addressMapper.selectCount(baseUserWrapper(platformUserId));
        boolean defaultAddress = Boolean.TRUE.equals(dto.getIsDefault()) || addressCount == 0;

        if (defaultAddress) {
            addressMapper.clearDefaultByPlatformUserId(platformUserId);
        }

        UserShippingAddress address = new UserShippingAddress();
        fillAddress(address, dto);
        address.setPlatformUserId(platformUserId);
        address.setIsDefault(defaultAddress ? 1 : 0);
        address.setDeleted(0);
        addressMapper.insert(address);
        return address;
    }

    /**
     * 更新地址。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserShippingAddress update(Long platformUserId, Long addressId, UserShippingAddressDTO dto) {
        validateAddress(dto);
        UserShippingAddress address = requireOwnedAddress(platformUserId, addressId);
        Boolean requestedDefault = dto.getIsDefault();

        if (Boolean.TRUE.equals(requestedDefault)) {
            addressMapper.clearDefaultByPlatformUserId(platformUserId);
            address.setIsDefault(1);
        } else if (Boolean.FALSE.equals(requestedDefault)
                && Integer.valueOf(1).equals(address.getIsDefault())) {
            ensureCanCancelDefault(platformUserId);
            address.setIsDefault(0);
        }

        fillAddress(address, dto);
        addressMapper.updateById(address);
        return address;
    }

    /**
     * 删除地址。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long platformUserId, Long addressId) {
        UserShippingAddress address = requireOwnedAddress(platformUserId, addressId);
        boolean wasDefault = Integer.valueOf(1).equals(address.getIsDefault());
        address.setDeleted(1);
        addressMapper.updateById(address);

        if (wasDefault) {
            promoteFirstAddressAsDefault(platformUserId);
        }
    }

    /**
     * 设置默认地址。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserShippingAddress setDefault(Long platformUserId, Long addressId) {
        UserShippingAddress address = requireOwnedAddress(platformUserId, addressId);
        addressMapper.clearDefaultByPlatformUserId(platformUserId);
        address.setIsDefault(1);
        addressMapper.updateById(address);
        return address;
    }

    private LambdaQueryWrapper<UserShippingAddress> baseUserWrapper(Long platformUserId) {
        return new LambdaQueryWrapper<UserShippingAddress>()
                .eq(UserShippingAddress::getPlatformUserId, platformUserId)
                .eq(UserShippingAddress::getDeleted, 0);
    }

    private UserShippingAddress requireOwnedAddress(Long platformUserId, Long addressId) {
        UserShippingAddress address = addressMapper.selectById(addressId);
        if (address == null
                || !platformUserId.equals(address.getPlatformUserId())
                || Integer.valueOf(1).equals(address.getDeleted())) {
            throw new BusinessException("收货地址不存在");
        }
        return address;
    }

    private void promoteFirstAddressAsDefault(Long platformUserId) {
        List<UserShippingAddress> remainingAddresses = addressMapper.selectList(baseUserWrapper(platformUserId)
                .orderByDesc(UserShippingAddress::getUpdateTime)
                .orderByDesc(UserShippingAddress::getId));
        if (remainingAddresses.isEmpty()) {
            return;
        }

        UserShippingAddress nextDefault = remainingAddresses.get(0);
        nextDefault.setIsDefault(1);
        addressMapper.updateById(nextDefault);
    }

    private void ensureCanCancelDefault(Long platformUserId) {
        long activeAddressCount = addressMapper.selectCount(baseUserWrapper(platformUserId));
        if (activeAddressCount <= 1) {
            throw new BusinessException("至少保留一个默认地址");
        }
    }

    private void fillAddress(UserShippingAddress address, UserShippingAddressDTO dto) {
        address.setReceiverName(trim(dto.getReceiverName()));
        address.setPhone(trim(dto.getPhone()));
        address.setProvince(trim(dto.getProvince()));
        address.setCity(trim(dto.getCity()));
        address.setDistrict(trim(dto.getDistrict()));
        address.setDetail(trim(dto.getDetail()));
    }

    private void validateAddress(UserShippingAddressDTO dto) {
        String phone = trim(dto.getPhone());
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException("手机号不能为空");
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("手机号格式不正确");
        }
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
