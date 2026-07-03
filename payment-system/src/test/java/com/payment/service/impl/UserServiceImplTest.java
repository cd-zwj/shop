package com.payment.service.impl;

import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.payment.common.BusinessException;
import com.payment.config.AuthStpKit;
import com.payment.config.RbacPrincipalType;
import com.payment.dto.LoginDTO;
import com.payment.entity.PlatformUser;
import com.payment.mapper.PlatformUserMapper;
import com.payment.mapper.RoleMapper;
import com.payment.mapper.UserMapper;
import com.payment.mapper.UserRoleMapper;
import com.payment.util.AuthLoginIdHelper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    @Test
    void loginAdminUsesPlatformUserIdAsRbacPrincipal() {
        PlatformUserMapper platformUserMapper = mock(PlatformUserMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        PlatformUser admin = new PlatformUser();
        admin.setId(99L);
        admin.setUsername("admin");
        admin.setPasswordHash(encoder.encode("admin123"));
        admin.setStatus(1);
        admin.setDeleted(0);

        when(platformUserMapper.selectOne(any(Wrapper.class))).thenReturn(admin);
        when(roleMapper.selectRoleCodesByPrincipal(99L, RbacPrincipalType.ADMIN)).thenReturn(List.of("admin"));

        UserServiceImpl service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userRoleMapper", mock(UserRoleMapper.class));
        ReflectionTestUtils.setField(service, "userMapper", mock(UserMapper.class));
        ReflectionTestUtils.setField(service, "platformUserMapper", platformUserMapper);
        ReflectionTestUtils.setField(service, "roleMapper", roleMapper);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("admin123");

        SaTokenContextMockUtil.setMockContext(() -> {
            String token = service.loginadmin(dto);

            assertThat(token).isNotBlank();
            assertThat(AuthStpKit.ADMIN.getLoginId()).isEqualTo(AuthLoginIdHelper.admin(99L));
            assertThat(AuthStpKit.ADMIN.getSession().get("platformUserId")).isEqualTo(99L);
            assertThat(AuthStpKit.ADMIN.getSession().get("userId")).isEqualTo(99L);
            AuthStpKit.ADMIN.logout();
        });
    }

    @Test
    void loginAdminRejectsPlatformUserWithoutAdminRole() {
        PlatformUserMapper platformUserMapper = mock(PlatformUserMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        PlatformUser platformUser = new PlatformUser();
        platformUser.setId(99L);
        platformUser.setUsername("merchant_user");
        platformUser.setPasswordHash(encoder.encode("admin123"));
        platformUser.setStatus(1);
        platformUser.setDeleted(0);

        when(platformUserMapper.selectOne(any(Wrapper.class))).thenReturn(platformUser);
        when(roleMapper.selectRoleCodesByPrincipal(99L, RbacPrincipalType.ADMIN)).thenReturn(List.of("user"));

        UserServiceImpl service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userRoleMapper", mock(UserRoleMapper.class));
        ReflectionTestUtils.setField(service, "userMapper", mock(UserMapper.class));
        ReflectionTestUtils.setField(service, "platformUserMapper", platformUserMapper);
        ReflectionTestUtils.setField(service, "roleMapper", roleMapper);

        LoginDTO dto = new LoginDTO();
        dto.setUsername("merchant_user");
        dto.setPassword("admin123");

        SaTokenContextMockUtil.setMockContext(() ->
                assertThatThrownBy(() -> service.loginadmin(dto))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage("用户权限不足,该用户不是管理员")
        );
    }
}
