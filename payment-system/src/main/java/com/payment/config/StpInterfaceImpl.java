package com.payment.config;

import cn.dev33.satoken.stp.StpInterface;
import com.payment.mapper.PermissionMapper;
import com.payment.mapper.RoleMapper;
import com.payment.util.AuthLoginIdHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Sa-Token 权限认证接口实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        try {
            Long userId = parseUserId(loginId, loginType);
            String principalType = RbacPrincipalType.fromLoginType(loginType);
            Set<String> permissions = new LinkedHashSet<>();
            addAll(permissions, permissionMapper.selectPermissionCodesByPrincipal(userId, principalType));
            addAll(permissions, permissionMapper.selectExtraPermissionCodesByPrincipal(userId, principalType));
            return new ArrayList<>(permissions);
        } catch (Exception e) {
            log.warn("Failed to load Sa-Token permissions, loginType={}, loginId={}", loginType, loginId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        try {
            Long userId = parseUserId(loginId, loginType);
            String principalType = RbacPrincipalType.fromLoginType(loginType);
            List<String> roles = roleMapper.selectRoleCodesByPrincipal(userId, principalType);
            return roles == null ? new ArrayList<>() : roles;
        } catch (Exception e) {
            log.warn("Failed to load Sa-Token roles, loginType={}, loginId={}", loginType, loginId, e);
            return new ArrayList<>();
        }
    }

    private Long parseUserId(Object loginId, String loginType) {
        return AuthLoginIdHelper.parse(loginId, loginType);
    }

    private void addAll(Set<String> target, List<String> source) {
        if (source == null) {
            return;
        }
        for (String permission : source) {
            if (permission != null && !permission.isBlank()) {
                target.add(permission);
            }
        }
    }
}
