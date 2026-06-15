package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.mapper.PermissionMapper;
import com.payment.mapper.UserMapper;
import com.payment.mapper.UserPermissionMapper;
import com.payment.service.PermissionCacheInvalidationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserPermissionServiceImplTest {

    @Test
    void grantPermissionShouldInvalidateUserCacheAfterInsert() {
        UserPermissionMapper userPermissionMapper = mock(UserPermissionMapper.class);
        PermissionCacheInvalidationService cacheInvalidationService = mock(PermissionCacheInvalidationService.class);
        UserPermissionServiceImpl service = service(userPermissionMapper, cacheInvalidationService);
        when(userPermissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        service.grantPermission(100L, 12L);

        verify(userPermissionMapper).insert(any());
        verify(cacheInvalidationService).invalidateUser(100L);
    }

    @Test
    void grantPermissionShouldNotInvalidateCacheWhenRelationAlreadyExists() {
        UserPermissionMapper userPermissionMapper = mock(UserPermissionMapper.class);
        PermissionCacheInvalidationService cacheInvalidationService = mock(PermissionCacheInvalidationService.class);
        UserPermissionServiceImpl service = service(userPermissionMapper, cacheInvalidationService);
        when(userPermissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        service.grantPermission(100L, 12L);

        verify(userPermissionMapper, never()).insert(any());
        verify(cacheInvalidationService, never()).invalidateUser(any());
    }

    @Test
    void revokePermissionShouldInvalidateUserCacheAfterDelete() {
        UserPermissionMapper userPermissionMapper = mock(UserPermissionMapper.class);
        PermissionCacheInvalidationService cacheInvalidationService = mock(PermissionCacheInvalidationService.class);
        UserPermissionServiceImpl service = service(userPermissionMapper, cacheInvalidationService);

        service.revokePermission(100L, 12L);

        verify(userPermissionMapper).deleteByUserIdAndPermissionId(100L, 12L);
        verify(cacheInvalidationService).invalidateUser(100L);
    }

    @Test
    void setUserPermissionsShouldInvalidateUserCacheAfterReplace() {
        UserPermissionMapper userPermissionMapper = mock(UserPermissionMapper.class);
        PermissionCacheInvalidationService cacheInvalidationService = mock(PermissionCacheInvalidationService.class);
        UserPermissionServiceImpl service = service(userPermissionMapper, cacheInvalidationService);

        service.setUserPermissions(100L, List.of(12L, 13L));

        verify(userPermissionMapper).deleteByUserId(100L);
        verify(userPermissionMapper, times(2)).insert(any());
        verify(cacheInvalidationService).invalidateUser(100L);
    }

    private UserPermissionServiceImpl service(UserPermissionMapper userPermissionMapper,
                                              PermissionCacheInvalidationService cacheInvalidationService) {
        return new UserPermissionServiceImpl(userPermissionMapper, mock(PermissionMapper.class),
                mock(UserMapper.class), cacheInvalidationService);
    }
}
