package com.payment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.dto.UserPermissionVO;
import com.payment.entity.User;
import com.payment.entity.UserPermission;
import com.payment.mapper.PermissionMapper;
import com.payment.mapper.UserMapper;
import com.payment.mapper.UserPermissionMapper;
import com.payment.service.UserPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserPermissionServiceImpl implements UserPermissionService {

    @Autowired
    private UserPermissionMapper userPermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private UserMapper userMapper;

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

        clearUserSession(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokePermission(Long userId, Long permissionId) {
        userPermissionMapper.deleteByUserIdAndPermissionId(userId, permissionId);
        clearUserSession(userId);
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

        clearUserSession(userId);
    }

    private void clearUserSession(Long userId) {
        try {
            // Get session if exists (false = don't create if not exists)
            cn.dev33.satoken.session.SaSession session = StpUtil.getSessionByLoginId(userId, false);
            if (session != null) {
                session.delete("permissions");
            }
        } catch (Exception e) {
            // Ignore errors during session clearing
        }
    }
}
