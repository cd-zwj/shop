package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.dto.UserPermissionVO;
import com.payment.entity.User;
import com.payment.entity.UserPermission;
import com.payment.mapper.PermissionMapper;
import com.payment.mapper.UserMapper;
import com.payment.mapper.UserPermissionMapper;
import com.payment.service.PermissionCacheInvalidationService;
import com.payment.service.UserPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserPermissionServiceImpl implements UserPermissionService {

    private final UserPermissionMapper userPermissionMapper;
    private final PermissionMapper permissionMapper;
    private final UserMapper userMapper;
    private final PermissionCacheInvalidationService permissionCacheInvalidationService;

    public UserPermissionServiceImpl(UserPermissionMapper userPermissionMapper,
                                     PermissionMapper permissionMapper,
                                     UserMapper userMapper,
                                     PermissionCacheInvalidationService permissionCacheInvalidationService) {
        this.userPermissionMapper = userPermissionMapper;
        this.permissionMapper = permissionMapper;
        this.userMapper = userMapper;
        this.permissionCacheInvalidationService = permissionCacheInvalidationService;
    }

    @Override
    public UserPermissionVO getUserPermissions(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        UserPermissionVO vo = new UserPermissionVO();
        vo.setUserId(userId);
        vo.setUsername(user.getUsername());

        // Role permissions
        List<String> rolePermissions = permissionMapper.selectPermissionCodesByUserId(userId);
        vo.setRolePermissions(rolePermissions != null ? rolePermissions : new ArrayList<>());

        // Extra permissions
        List<String> extraPermissions = permissionMapper.selectExtraPermissionCodesByUserId(userId);
        vo.setExtraPermissions(extraPermissions != null ? extraPermissions : new ArrayList<>());

        // All permissions
        List<String> allPermissions = new ArrayList<>(vo.getRolePermissions());
        allPermissions.addAll(vo.getExtraPermissions());
        // Deduplicate
        vo.setAllPermissions(allPermissions.stream().distinct().collect(Collectors.toList()));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantPermission(Long userId, Long permissionId) {
        // Check if exists
        Long count = userPermissionMapper.selectCount(new LambdaQueryWrapper<UserPermission>()
                .eq(UserPermission::getUserId, userId)
                .eq(UserPermission::getPermissionId, permissionId));

        if (count > 0) {
            return;
        }

        UserPermission up = new UserPermission();
        up.setUserId(userId);
        up.setPermissionId(permissionId);
        userPermissionMapper.insert(up);

        permissionCacheInvalidationService.invalidateUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokePermission(Long userId, Long permissionId) {
        userPermissionMapper.deleteByUserIdAndPermissionId(userId, permissionId);
        permissionCacheInvalidationService.invalidateUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserPermissions(Long userId, List<Long> permissionIds) {
        // Delete all existing extra permissions
        userPermissionMapper.deleteByUserId(userId);

        // Insert new ones
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                UserPermission up = new UserPermission();
                up.setUserId(userId);
                up.setPermissionId(permissionId);
                userPermissionMapper.insert(up);
            }
        }

        permissionCacheInvalidationService.invalidateUser(userId);
    }
}
