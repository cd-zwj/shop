package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.config.RbacPrincipalType;
import com.payment.dto.UserPermissionVO;
import com.payment.entity.PlatformUser;
import com.payment.entity.UserPermission;
import com.payment.mapper.PermissionMapper;
import com.payment.mapper.PlatformUserMapper;
import com.payment.mapper.UserPermissionMapper;
import com.payment.service.PermissionCacheInvalidationService;
import com.payment.service.UserPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户权限服务实现类。
 */
@Service
public class UserPermissionServiceImpl implements UserPermissionService {

    private final UserPermissionMapper userPermissionMapper;
    private final PermissionMapper permissionMapper;
    private final PlatformUserMapper platformUserMapper;
    private final PermissionCacheInvalidationService permissionCacheInvalidationService;

    public UserPermissionServiceImpl(UserPermissionMapper userPermissionMapper,
                                     PermissionMapper permissionMapper,
                                     PlatformUserMapper platformUserMapper,
                                     PermissionCacheInvalidationService permissionCacheInvalidationService) {
        this.userPermissionMapper = userPermissionMapper;
        this.permissionMapper = permissionMapper;
        this.platformUserMapper = platformUserMapper;
        this.permissionCacheInvalidationService = permissionCacheInvalidationService;
    }

    @Override
    public UserPermissionVO getUserPermissions(Long userId) {
        PlatformUser platformUser = platformUserMapper.selectById(userId);
        if (platformUser == null) {
            throw new BusinessException("用户不存在");
        }

        UserPermissionVO vo = new UserPermissionVO();
        vo.setUserId(userId);
        vo.setUsername(platformUser.getUsername());

        List<String> rolePermissions = permissionMapper.selectPermissionCodesByPrincipal(userId, RbacPrincipalType.PLATFORM);
        vo.setRolePermissions(rolePermissions != null ? rolePermissions : new ArrayList<>());

        List<String> extraPermissions = permissionMapper.selectExtraPermissionCodesByPrincipal(userId, RbacPrincipalType.PLATFORM);
        vo.setExtraPermissions(extraPermissions != null ? extraPermissions : new ArrayList<>());

        List<String> allPermissions = new ArrayList<>(vo.getRolePermissions());
        allPermissions.addAll(vo.getExtraPermissions());
        vo.setAllPermissions(allPermissions.stream().distinct().collect(Collectors.toList()));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantPermission(Long userId, Long permissionId) {
        Long count = userPermissionMapper.selectCount(new LambdaQueryWrapper<UserPermission>()
                .eq(UserPermission::getPrincipalType, RbacPrincipalType.PLATFORM)
                .eq(UserPermission::getUserId, userId)
                .eq(UserPermission::getPermissionId, permissionId));

        if (count > 0) {
            return;
        }

        UserPermission up = new UserPermission();
        up.setPrincipalType(RbacPrincipalType.PLATFORM);
        up.setUserId(userId);
        up.setPermissionId(permissionId);
        userPermissionMapper.insert(up);

        permissionCacheInvalidationService.invalidateUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokePermission(Long userId, Long permissionId) {
        userPermissionMapper.deleteByPrincipalAndPermissionId(RbacPrincipalType.PLATFORM, userId, permissionId);
        permissionCacheInvalidationService.invalidateUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserPermissions(Long userId, List<Long> permissionIds) {
        userPermissionMapper.deleteByPrincipal(RbacPrincipalType.PLATFORM, userId);

        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                UserPermission up = new UserPermission();
                up.setPrincipalType(RbacPrincipalType.PLATFORM);
                up.setUserId(userId);
                up.setPermissionId(permissionId);
                userPermissionMapper.insert(up);
            }
        }

        permissionCacheInvalidationService.invalidateUser(userId);
    }
}
