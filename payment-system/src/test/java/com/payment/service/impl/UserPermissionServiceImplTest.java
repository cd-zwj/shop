package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.config.RbacPrincipalType;
import com.payment.dto.UserPermissionVO;
import com.payment.entity.PlatformUser;
import com.payment.entity.UserPermission;
import com.payment.mapper.PermissionMapper;
import com.payment.mapper.PlatformUserMapper;
import com.payment.mapper.UserPermissionMapper;
import com.payment.service.PermissionCacheInvalidationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserPermissionServiceImplTest {

    @Test
    void getUserPermissionsShouldLoadPlatformUserByPlatformUserId() {
        UserPermissionMapper userPermissionMapper = mock(UserPermissionMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        PlatformUserMapper platformUserMapper = mock(PlatformUserMapper.class);
        PermissionCacheInvalidationService cacheInvalidationService = mock(PermissionCacheInvalidationService.class);

        PlatformUser platformUser = new PlatformUser();
        platformUser.setId(200L);
        platformUser.setUsername("platform-alice");
        when(platformUserMapper.selectById(200L)).thenReturn(platformUser);
        when(permissionMapper.selectPermissionCodesByPrincipal(200L, RbacPrincipalType.PLATFORM))
                .thenReturn(List.of("user:read"));
        when(permissionMapper.selectExtraPermissionCodesByPrincipal(200L, RbacPrincipalType.PLATFORM))
                .thenReturn(List.of("coupon:grant"));

        UserPermissionServiceImpl service = new UserPermissionServiceImpl(
                userPermissionMapper,
                permissionMapper,
                platformUserMapper,
                cacheInvalidationService
        );

        UserPermissionVO result = service.getUserPermissions(200L);

        assertThat(result.getUserId()).isEqualTo(200L);
        assertThat(result.getUsername()).isEqualTo("platform-alice");
        assertThat(result.getRolePermissions()).containsExactly("user:read");
        assertThat(result.getExtraPermissions()).containsExactly("coupon:grant");
        assertThat(result.getAllPermissions()).containsExactly("user:read", "coupon:grant");
    }

    @Test
    void grantPermissionShouldInvalidateUserCacheAfterInsert() {
        UserPermissionMapper userPermissionMapper = mock(UserPermissionMapper.class);
        PermissionCacheInvalidationService cacheInvalidationService = mock(PermissionCacheInvalidationService.class);
        UserPermissionServiceImpl service = service(userPermissionMapper, cacheInvalidationService);
        when(userPermissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        service.grantPermission(100L, 12L);

        ArgumentCaptor<UserPermission> captor = ArgumentCaptor.forClass(UserPermission.class);
        verify(userPermissionMapper).insert(captor.capture());
        assertThat(captor.getValue().getPrincipalType()).isEqualTo(RbacPrincipalType.PLATFORM);
        assertThat(captor.getValue().getUserId()).isEqualTo(100L);
        assertThat(captor.getValue().getPermissionId()).isEqualTo(12L);
        verify(cacheInvalidationService).invalidateUser(100L);
    }

    @Test
    void grantPermissionShouldNotInvalidateCacheWhenRelationAlreadyExists() {
        UserPermissionMapper userPermissionMapper = mock(UserPermissionMapper.class);
        PermissionCacheInvalidationService cacheInvalidationService = mock(PermissionCacheInvalidationService.class);
        UserPermissionServiceImpl service = service(userPermissionMapper, cacheInvalidationService);
        when(userPermissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        service.grantPermission(100L, 12L);

        verify(userPermissionMapper, never()).insert(any(UserPermission.class));
        verify(cacheInvalidationService, never()).invalidateUser(any());
    }

    @Test
    void revokePermissionShouldInvalidateUserCacheAfterDelete() {
        UserPermissionMapper userPermissionMapper = mock(UserPermissionMapper.class);
        PermissionCacheInvalidationService cacheInvalidationService = mock(PermissionCacheInvalidationService.class);
        UserPermissionServiceImpl service = service(userPermissionMapper, cacheInvalidationService);

        service.revokePermission(100L, 12L);

        verify(userPermissionMapper).deleteByPrincipalAndPermissionId(RbacPrincipalType.PLATFORM, 100L, 12L);
        verify(cacheInvalidationService).invalidateUser(100L);
    }

    @Test
    void setUserPermissionsShouldInvalidateUserCacheAfterReplace() {
        UserPermissionMapper userPermissionMapper = mock(UserPermissionMapper.class);
        PermissionCacheInvalidationService cacheInvalidationService = mock(PermissionCacheInvalidationService.class);
        UserPermissionServiceImpl service = service(userPermissionMapper, cacheInvalidationService);

        service.setUserPermissions(100L, List.of(12L, 13L));

        verify(userPermissionMapper).deleteByPrincipal(RbacPrincipalType.PLATFORM, 100L);
        verify(userPermissionMapper, times(2)).insert(any(UserPermission.class));
        verify(cacheInvalidationService).invalidateUser(100L);
    }

    private UserPermissionServiceImpl service(UserPermissionMapper userPermissionMapper,
                                              PermissionCacheInvalidationService cacheInvalidationService) {
        return new UserPermissionServiceImpl(userPermissionMapper, mock(PermissionMapper.class),
                mock(PlatformUserMapper.class), cacheInvalidationService);
    }
}
