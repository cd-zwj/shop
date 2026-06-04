package com.payment.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.PlatformAuthProviderDTO;
import com.payment.dto.PlatformAuthProviderVO;
import com.payment.entity.PlatformAuthProvider;
import com.payment.mapper.PlatformAuthProviderMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 第三方登录方式管理测试类，用于验证第三方登录方式管理相关逻辑。
 */
class PlatformAuthProviderAdminServiceImplTest {

    /**
     * 创建渠道ShouldNormalize编码AndPersist。
     */
    @Test
    void createProviderShouldNormalizeCodeAndPersist() {
        PlatformAuthProviderMapper mapper = mock(PlatformAuthProviderMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);

        PlatformAuthProviderAdminServiceImpl service = new PlatformAuthProviderAdminServiceImpl(mapper);
        PlatformAuthProviderDTO dto = new PlatformAuthProviderDTO();
        dto.setProviderCode(" wechat ");
        dto.setProviderName("微信登录");
        dto.setSortOrder(10);

        PlatformAuthProviderVO result = service.createProvider(dto);

        assertEquals("WECHAT", result.getProviderCode());
        assertEquals("微信登录", result.getProviderName());
        assertEquals(1, result.getStatus());
        verify(mapper).insert(any(PlatformAuthProvider.class));
    }

    /**
     * 创建渠道ShouldRejectDuplicate编码。
     */
    @Test
    void createProviderShouldRejectDuplicateCode() {
        PlatformAuthProviderMapper mapper = mock(PlatformAuthProviderMapper.class);
        PlatformAuthProvider existing = new PlatformAuthProvider();
        existing.setId(1L);
        existing.setProviderCode("QQ");
        when(mapper.selectOne(any())).thenReturn(existing);

        PlatformAuthProviderAdminServiceImpl service = new PlatformAuthProviderAdminServiceImpl(mapper);
        PlatformAuthProviderDTO dto = new PlatformAuthProviderDTO();
        dto.setProviderCode("qq");
        dto.setProviderName("QQ 登录");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createProvider(dto));

        assertEquals("第三方登录方式编码已存在", exception.getMessage());
    }

    /**
     * 查询渠道ShouldMapPage记录。
     */
    @Test
    void listProvidersShouldMapPageRecords() {
        PlatformAuthProviderMapper mapper = mock(PlatformAuthProviderMapper.class);
        PlatformAuthProvider provider = new PlatformAuthProvider();
        provider.setId(2L);
        provider.setProviderCode("APPLE");
        provider.setProviderName("Apple 登录");
        provider.setStatus(1);
        provider.setSortOrder(5);

        Page<PlatformAuthProvider> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(provider));
        when(mapper.selectPage(any(), any())).thenReturn(page);

        PlatformAuthProviderAdminServiceImpl service = new PlatformAuthProviderAdminServiceImpl(mapper);
        Page<PlatformAuthProviderVO> result = service.listProviders(1, 10, "apple", 1);

        assertEquals(1, result.getRecords().size());
        assertEquals("APPLE", result.getRecords().get(0).getProviderCode());
    }
}
