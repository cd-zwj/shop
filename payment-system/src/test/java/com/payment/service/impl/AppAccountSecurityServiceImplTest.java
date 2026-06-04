package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.dto.AppChangePasswordDTO;
import com.payment.dto.AppAccountSecurityVO;
import com.payment.entity.PlatformAuthProvider;
import com.payment.entity.PlatformUser;
import com.payment.entity.PlatformUserAuth;
import com.payment.mapper.PlatformAuthProviderMapper;
import com.payment.mapper.PlatformUserAuthMapper;
import com.payment.mapper.PlatformUserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户端账号安全服务测试类，用于验证手机号、邮箱、密码和第三方绑定状态。
 */
class AppAccountSecurityServiceImplTest {

    /**
     * 获取安全摘要Should返回绑定状态And第三方绑定状态。
     */
    @Test
    void getSecuritySummaryShouldExposeBindingStates() {
        PlatformUserMapper userMapper = mock(PlatformUserMapper.class);
        PlatformAuthProviderMapper providerMapper = mock(PlatformAuthProviderMapper.class);
        PlatformUserAuthMapper authMapper = mock(PlatformUserAuthMapper.class);
        AppAccountSecurityServiceImpl service = new AppAccountSecurityServiceImpl(
                userMapper,
                providerMapper,
                authMapper,
                new BCryptPasswordEncoder()
        );

        PlatformUser user = new PlatformUser();
        user.setId(100L);
        user.setPhone("13800138000");
        user.setEmail("demo@example.com");
        user.setEmailVerified(1);
        user.setPasswordHash("encoded");
        user.setDeleted(0);
        when(userMapper.selectById(100L)).thenReturn(user);

        PlatformAuthProvider wechat = buildProvider(1L, "WECHAT", "微信");
        PlatformAuthProvider alipay = buildProvider(2L, "ALIPAY", "支付宝");
        when(providerMapper.selectList(any())).thenReturn(List.of(wechat, alipay));

        PlatformUserAuth auth = new PlatformUserAuth();
        auth.setProviderId(1L);
        auth.setPlatformUserId(100L);
        when(authMapper.selectList(any())).thenReturn(List.of(auth));

        AppAccountSecurityVO result = service.getSecuritySummary(100L);

        assertEquals(true, result.getPhone().getBound());
        assertEquals("138****8000", result.getPhone().getMaskedValue());
        assertEquals(true, result.getEmail().getBound());
        assertEquals("de***@example.com", result.getEmail().getMaskedValue());
        assertEquals(true, result.getPassword().getSet());
        assertEquals(2, result.getThirdPartyBindings().size());
        assertEquals(true, result.getThirdPartyBindings().get(0).getBound());
        assertEquals(false, result.getThirdPartyBindings().get(1).getBound());
    }

    /**
     * 修改密码Should校验旧密码And写入新密码Hash。
     */
    @Test
    void changePasswordShouldValidateOldPasswordAndStoreNewHash() {
        PlatformUserMapper userMapper = mock(PlatformUserMapper.class);
        PlatformAuthProviderMapper providerMapper = mock(PlatformAuthProviderMapper.class);
        PlatformUserAuthMapper authMapper = mock(PlatformUserAuthMapper.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        AppAccountSecurityServiceImpl service = new AppAccountSecurityServiceImpl(userMapper, providerMapper, authMapper, encoder);

        PlatformUser user = new PlatformUser();
        user.setId(100L);
        user.setPasswordHash(encoder.encode("old-secret"));
        user.setDeleted(0);
        when(userMapper.selectById(100L)).thenReturn(user);

        AppChangePasswordDTO dto = new AppChangePasswordDTO();
        dto.setOldPassword("old-secret");
        dto.setNewPassword("new-secret");

        service.changePassword(100L, dto);

        ArgumentCaptor<PlatformUser> captor = ArgumentCaptor.forClass(PlatformUser.class);
        verify(userMapper).updateById(captor.capture());
        assertTrue(encoder.matches("new-secret", captor.getValue().getPasswordHash()));
    }

    /**
     * 修改密码Should拒绝错误旧密码。
     */
    @Test
    void changePasswordShouldRejectWrongOldPassword() {
        PlatformUserMapper userMapper = mock(PlatformUserMapper.class);
        PlatformAuthProviderMapper providerMapper = mock(PlatformAuthProviderMapper.class);
        PlatformUserAuthMapper authMapper = mock(PlatformUserAuthMapper.class);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        AppAccountSecurityServiceImpl service = new AppAccountSecurityServiceImpl(userMapper, providerMapper, authMapper, encoder);

        PlatformUser user = new PlatformUser();
        user.setId(100L);
        user.setPasswordHash(encoder.encode("old-secret"));
        user.setDeleted(0);
        when(userMapper.selectById(100L)).thenReturn(user);

        AppChangePasswordDTO dto = new AppChangePasswordDTO();
        dto.setOldPassword("wrong-secret");
        dto.setNewPassword("new-secret");

        assertThrows(BusinessException.class, () -> service.changePassword(100L, dto));
    }

    private PlatformAuthProvider buildProvider(Long id, String code, String name) {
        PlatformAuthProvider provider = new PlatformAuthProvider();
        provider.setId(id);
        provider.setProviderCode(code);
        provider.setProviderName(name);
        provider.setStatus(1);
        return provider;
    }
}
