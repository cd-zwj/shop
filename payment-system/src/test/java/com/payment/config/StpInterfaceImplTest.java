package com.payment.config;

import com.payment.mapper.PermissionMapper;
import com.payment.mapper.RoleMapper;
import com.payment.util.AuthLoginIdHelper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StpInterfaceImplTest {

    private final RoleMapper roleMapper = mock(RoleMapper.class);
    private final PermissionMapper permissionMapper = mock(PermissionMapper.class);
    private final StpInterfaceImpl stpInterface = new StpInterfaceImpl(roleMapper, permissionMapper);

    @Test
    void getPermissionListMergesRoleAndExtraPermissions() {
        when(permissionMapper.selectPermissionCodesByPrincipal(10L, RbacPrincipalType.ADMIN))
                .thenReturn(List.of("admin:user:list", "admin:user:update"));
        when(permissionMapper.selectExtraPermissionCodesByPrincipal(10L, RbacPrincipalType.ADMIN))
                .thenReturn(List.of("admin:user:update", "admin:dashboard"));

        List<String> permissions = stpInterface.getPermissionList(AuthLoginIdHelper.admin(10L), AuthStpKit.ADMIN_TYPE);

        assertThat(permissions).containsExactly("admin:user:list", "admin:user:update", "admin:dashboard");
    }

    @Test
    void getPermissionListRejectsMismatchedLoginIdPrefix() {
        List<String> permissions = stpInterface.getPermissionList(AuthLoginIdHelper.platform(10L), AuthStpKit.ADMIN_TYPE);

        assertThat(permissions).isEmpty();
        verify(permissionMapper, never()).selectPermissionCodesByPrincipal(10L, RbacPrincipalType.ADMIN);
        verify(permissionMapper, never()).selectExtraPermissionCodesByPrincipal(10L, RbacPrincipalType.ADMIN);
    }

    @Test
    void getRoleListUsesNamespacedLoginId() {
        when(roleMapper.selectRoleCodesByPrincipal(20L, RbacPrincipalType.MERCHANT)).thenReturn(List.of("merchant"));

        List<String> roles = stpInterface.getRoleList(AuthLoginIdHelper.merchant(20L), AuthStpKit.MERCHANT_TYPE);

        assertThat(roles).containsExactly("merchant");
    }

    @Test
    void sameNumericIdDoesNotReuseOtherPrincipalPermissions() {
        when(permissionMapper.selectPermissionCodesByPrincipal(1L, RbacPrincipalType.PLATFORM)).thenReturn(List.of("user:info"));
        when(permissionMapper.selectExtraPermissionCodesByPrincipal(1L, RbacPrincipalType.PLATFORM)).thenReturn(List.of());

        List<String> permissions = stpInterface.getPermissionList(AuthLoginIdHelper.platform(1L), AuthStpKit.PLATFORM_TYPE);

        assertThat(permissions).containsExactly("user:info");
        verify(permissionMapper, never()).selectPermissionCodesByPrincipal(1L, RbacPrincipalType.ADMIN);
        verify(permissionMapper, never()).selectExtraPermissionCodesByPrincipal(1L, RbacPrincipalType.ADMIN);
    }
}
