package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.common.BusinessException;
import com.payment.config.AuthStpKit;
import com.payment.config.RbacPrincipalType;
import com.payment.dto.LoginDTO;
import com.payment.dto.MiniProgramUserVO;
import com.payment.dto.WechatLoginDTO;
import com.payment.entity.PlatformUser;
import com.payment.entity.User;
import com.payment.mapper.PlatformUserMapper;
import com.payment.mapper.RoleMapper;
import com.payment.mapper.UserMapper;
import com.payment.mapper.UserRoleMapper;
import com.payment.service.UserService;
import com.payment.util.AuthLoginIdHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PlatformUserMapper platformUserMapper;

    @Autowired
    private RoleMapper roleMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public String login(LoginDTO dto) {
        User user = getByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("用户已被禁用");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        AuthStpKit.PLATFORM.login(AuthLoginIdHelper.platform(user.getId()));
        AuthStpKit.PLATFORM.getSession().set("user", user);
        AuthStpKit.PLATFORM.getSession().set("tenantId", user.getTenantId());
        AuthStpKit.PLATFORM.getSession().set("userType", user.getUserType());
        AuthStpKit.PLATFORM.getSession().set("username", user.getUsername());
        AuthStpKit.PLATFORM.getSession().set("platformUserId", user.getId());
        AuthStpKit.PLATFORM.getSession().set("platformUsername", user.getUsername());
        return AuthStpKit.PLATFORM.getTokenValue();
    }

    @Override
    public String loginadmin(LoginDTO dto){
        PlatformUser admin = platformUserMapper.selectOne(new LambdaQueryWrapper<PlatformUser>()
                .eq(PlatformUser::getUsername, dto.getUsername())
                .eq(PlatformUser::getDeleted, 0));
        if (admin == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() == 0) {
            throw new BusinessException("用户已被禁用");
        }
        if (!passwordEncoder.matches(dto.getPassword(), admin.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        List<String> roles = roleMapper.selectRoleCodesByPrincipal(admin.getId(), RbacPrincipalType.ADMIN);
        if (roles == null || !roles.contains("admin")) {
            throw new BusinessException("用户权限不足,该用户不是管理员");
        }

        AuthStpKit.ADMIN.login(AuthLoginIdHelper.admin(admin.getId()));
        AuthStpKit.ADMIN.getSession().set("platformUser", admin);
        AuthStpKit.ADMIN.getSession().set("userType", 2);
        AuthStpKit.ADMIN.getSession().set("username", admin.getUsername());
        AuthStpKit.ADMIN.getSession().set("userId", admin.getId());
        AuthStpKit.ADMIN.getSession().set("platformUserId", admin.getId());
        AuthStpKit.ADMIN.getSession().set("platformUsername", admin.getUsername());
        return AuthStpKit.ADMIN.getTokenValue();
    }

    @Override
    public User getByUsername(String username) {
        Long tenantId = com.payment.util.TenantContextHolder.getTenantId();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, 0);
        if (tenantId != null) {
            wrapper.eq(User::getTenantId, tenantId);
        }
        return getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(User user) {
        User existUser = getByUsername(user.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setUserType(1);
        user.setStatus(1);

        save(user);

        userRoleMapper.insertUserRole(com.payment.config.RbacPrincipalType.PLATFORM, user.getId(), 1L);
        log.info("用户注册成功，已分配默认角色: userId={}, roleId=1", user.getId());

        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MiniProgramUserVO wechatLogin(WechatLoginDTO dto) {
        String openid = "wx_" + dto.getCode();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUsername, openid)
                .eq(User::getDeleted, 0);
        User user = getOne(wrapper);

        if (user == null) {
            user = new User();
            user.setUsername(openid);
            user.setPassword(passwordEncoder.encode(openid));
            user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : "微信用户");
            user.setAvatar(dto.getAvatar());
            user.setPhone(dto.getPhone());
            user.setUserType(1);
            user.setStatus(1);
            save(user);
        } else {
            if (StringUtils.hasText(dto.getNickname())) {
                user.setNickname(dto.getNickname());
            }
            if (StringUtils.hasText(dto.getAvatar())) {
                user.setAvatar(dto.getAvatar());
            }
            if (StringUtils.hasText(dto.getPhone())) {
                user.setPhone(dto.getPhone());
            }
            updateById(user);
        }

        AuthStpKit.PLATFORM.login(AuthLoginIdHelper.platform(user.getId()));
        AuthStpKit.PLATFORM.getSession().set("user", user);
        AuthStpKit.PLATFORM.getSession().set("tenantId", user.getTenantId());
        AuthStpKit.PLATFORM.getSession().set("userType", user.getUserType());
        AuthStpKit.PLATFORM.getSession().set("username", user.getUsername());
        AuthStpKit.PLATFORM.getSession().set("platformUserId", user.getId());
        AuthStpKit.PLATFORM.getSession().set("platformUsername", user.getUsername());

        MiniProgramUserVO userVO = new MiniProgramUserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setToken(AuthStpKit.PLATFORM.getTokenValue());

        return userVO;
    }
}
