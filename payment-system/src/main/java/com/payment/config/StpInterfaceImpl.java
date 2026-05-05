package com.payment.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.payment.mapper.PermissionMapper;
import com.payment.mapper.RoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义权限验证接口扩展
 * 从数据库查询用户的角色和权限
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 先从Session缓存获取
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) StpUtil.getSession().get("permissions");
        if (permissions != null) {
            return permissions;
        }

        // 从数据库查询
        try {
            Long userId = Long.valueOf(loginId.toString());
            permissions = permissionMapper.selectPermissionCodesByUserId(userId);
            if (permissions == null) {
                permissions = new ArrayList<>();
            }
            // 缓存到Session
            StpUtil.getSession().set("permissions", permissions);
            return permissions;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 返回一个账号所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 先从Session缓存获取
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) StpUtil.getSession().get("roles");
        if (roles != null) {
            return roles;
        }

        // 从数据库查询
        try {
            Long userId = Long.valueOf(loginId.toString());
            roles = roleMapper.selectRoleCodesByUserId(userId);
            if (roles == null) {
                roles = new ArrayList<>();
            }
            // 缓存到Session
            StpUtil.getSession().set("roles", roles);
            return roles;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
