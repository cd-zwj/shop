package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.dto.UserShippingAddressDTO;
import com.payment.entity.UserShippingAddress;
import com.payment.mapper.UserShippingAddressMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 用户收货地址服务测试类，用于验证地址增删改查和默认地址约束。
 */
class UserShippingAddressServiceImplTest {

    /**
     * 创建首个地址Should自动设为默认地址。
     */
    @Test
    void createFirstAddressShouldBecomeDefault() {
        UserShippingAddressMapper addressMapper = mock(UserShippingAddressMapper.class);
        UserShippingAddressServiceImpl service = new UserShippingAddressServiceImpl(addressMapper);

        when(addressMapper.selectCount(any())).thenReturn(0L);

        service.create(100L, buildDto(false, "科技园 1 号"));

        ArgumentCaptor<UserShippingAddress> captor = ArgumentCaptor.forClass(UserShippingAddress.class);
        verify(addressMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getPlatformUserId());
        assertEquals(1, captor.getValue().getIsDefault());
    }

    /**
     * 创建地址Should拒绝非法手机号且不写库。
     */
    @Test
    void createAddressShouldRejectInvalidPhoneBeforePersisting() {
        UserShippingAddressMapper addressMapper = mock(UserShippingAddressMapper.class);
        UserShippingAddressServiceImpl service = new UserShippingAddressServiceImpl(addressMapper);
        UserShippingAddressDTO dto = buildDto(true, "科技园 1 号");
        dto.setPhone("12345");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(100L, dto));

        assertEquals("手机号格式不正确", exception.getMessage());
        verifyNoInteractions(addressMapper);
    }

    /**
     * 设置默认地址Should清理同用户旧默认地址。
     */
    @Test
    void setDefaultShouldClearPreviousDefaultForSameUser() {
        UserShippingAddressMapper addressMapper = mock(UserShippingAddressMapper.class);
        UserShippingAddressServiceImpl service = new UserShippingAddressServiceImpl(addressMapper);

        UserShippingAddress address = buildAddress(9L, 100L, 0);
        when(addressMapper.selectById(9L)).thenReturn(address);

        service.setDefault(100L, 9L);

        verify(addressMapper).clearDefaultByPlatformUserId(100L);
        ArgumentCaptor<UserShippingAddress> captor = ArgumentCaptor.forClass(UserShippingAddress.class);
        verify(addressMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getIsDefault());
    }

    /**
     * 更新其他用户地址Should拒绝越权。
     */
    @Test
    void updateOtherUserAddressShouldBeRejected() {
        UserShippingAddressMapper addressMapper = mock(UserShippingAddressMapper.class);
        UserShippingAddressServiceImpl service = new UserShippingAddressServiceImpl(addressMapper);

        when(addressMapper.selectById(9L)).thenReturn(buildAddress(9L, 200L, 0));

        assertThrows(BusinessException.class, () -> service.update(100L, 9L, buildDto(false, "新地址")));
        verify(addressMapper, never()).updateById(any(com.payment.entity.UserShippingAddress.class));
    }

    /**
     * 更新地址Should拒绝非法手机号且不持久化。
     */
    @Test
    void updateAddressShouldRejectInvalidPhoneBeforePersisting() {
        UserShippingAddressMapper addressMapper = mock(UserShippingAddressMapper.class);
        UserShippingAddressServiceImpl service = new UserShippingAddressServiceImpl(addressMapper);
        UserShippingAddressDTO dto = buildDto(false, "科技园 2 号");
        dto.setPhone("1380013800x");

        when(addressMapper.selectById(9L)).thenReturn(buildAddress(9L, 100L, 0));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.update(100L, 9L, dto));

        assertEquals("手机号格式不正确", exception.getMessage());
        verify(addressMapper, never()).clearDefaultByPlatformUserId(anyLong());
        verify(addressMapper, never()).updateById(any(com.payment.entity.UserShippingAddress.class));
    }

    /**
     * 更新默认地址为非默认Should在存在其他地址时允许取消默认。
     */
    @Test
    void updateDefaultAddressShouldAllowCancelWhenOtherAddressExists() {
        UserShippingAddressMapper addressMapper = mock(UserShippingAddressMapper.class);
        UserShippingAddressServiceImpl service = new UserShippingAddressServiceImpl(addressMapper);

        when(addressMapper.selectById(9L)).thenReturn(buildAddress(9L, 100L, 1));
        when(addressMapper.selectCount(any())).thenReturn(2L);

        service.update(100L, 9L, buildDto(false, "科技园 2 号"));

        ArgumentCaptor<UserShippingAddress> captor = ArgumentCaptor.forClass(UserShippingAddress.class);
        verify(addressMapper, never()).clearDefaultByPlatformUserId(100L);
        verify(addressMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getIsDefault());
        assertEquals("科技园 2 号", captor.getValue().getDetail());
    }

    /**
     * 更新唯一默认地址为非默认Should拒绝取消默认。
     */
    @Test
    void updateOnlyDefaultAddressShouldRejectCancelDefault() {
        UserShippingAddressMapper addressMapper = mock(UserShippingAddressMapper.class);
        UserShippingAddressServiceImpl service = new UserShippingAddressServiceImpl(addressMapper);

        when(addressMapper.selectById(9L)).thenReturn(buildAddress(9L, 100L, 1));
        when(addressMapper.selectCount(any())).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.update(100L, 9L, buildDto(false, "科技园 2 号")));
        verify(addressMapper, never()).updateById(any(com.payment.entity.UserShippingAddress.class));
    }

    /**
     * 删除默认地址Should把剩余第一个地址提升为默认。
     */
    @Test
    void deleteDefaultAddressShouldPromoteNextAddress() {
        UserShippingAddressMapper addressMapper = mock(UserShippingAddressMapper.class);
        UserShippingAddressServiceImpl service = new UserShippingAddressServiceImpl(addressMapper);

        UserShippingAddress deletedAddress = buildAddress(1L, 100L, 1);
        UserShippingAddress nextAddress = buildAddress(2L, 100L, 0);
        when(addressMapper.selectById(1L)).thenReturn(deletedAddress);
        when(addressMapper.selectList(any())).thenReturn(List.of(nextAddress));

        service.delete(100L, 1L);

        ArgumentCaptor<UserShippingAddress> captor = ArgumentCaptor.forClass(UserShippingAddress.class);
        verify(addressMapper, never()).deleteById(anyLong());
        verify(addressMapper, times(2)).updateById(captor.capture());
        assertEquals(1L, captor.getAllValues().get(0).getId());
        assertEquals(1, captor.getAllValues().get(0).getDeleted());
        assertEquals(2L, captor.getAllValues().get(1).getId());
        assertEquals(1, captor.getAllValues().get(1).getIsDefault());
    }

    private UserShippingAddressDTO buildDto(boolean isDefault, String detail) {
        UserShippingAddressDTO dto = new UserShippingAddressDTO();
        dto.setReceiverName("张三");
        dto.setPhone("13800138000");
        dto.setProvince("广东省");
        dto.setCity("深圳市");
        dto.setDistrict("南山区");
        dto.setDetail(detail);
        dto.setIsDefault(isDefault);
        return dto;
    }

    private UserShippingAddress buildAddress(Long id, Long platformUserId, Integer isDefault) {
        UserShippingAddress address = new UserShippingAddress();
        address.setId(id);
        address.setPlatformUserId(platformUserId);
        address.setReceiverName("张三");
        address.setPhone("13800138000");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDistrict("南山区");
        address.setDetail("科技园 1 号");
        address.setIsDefault(isDefault);
        address.setDeleted(0);
        return address;
    }
}
