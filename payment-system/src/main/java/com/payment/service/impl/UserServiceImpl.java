package com.payment.service.impl;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.common.BusinessException;
import com.payment.dto.LoginDTO;
import com.payment.dto.MiniProgramUserVO;
import com.payment.dto.WechatLoginDTO;
import com.payment.entity.User;
import com.payment.mapper.UserMapper;
import com.payment.mapper.UserRoleMapper;
import com.payment.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserRoleMapper userRoleMapper;

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

        // Sa-Token 登录
        StpUtil.login(user.getId());

        // 存储用户信息到Session
        StpUtil.getSession().set("user", user);
        StpUtil.getSession().set("tenantId", user.getTenantId());
        StpUtil.getSession().set("userType", user.getUserType());

        // 返回Token
        return StpUtil.getTokenValue();
    }

    @Override
    public String loginadmin(LoginDTO dto){
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
        if (!user.getUserType().equals(2)){
            throw new BusinessException("用户权限不足,该用户不是管理员");
        }

        // Sa-Token 登录
        StpUtil.login(user.getId());

        // 存储用户信息到Session
        StpUtil.getSession().set("user", user);
        StpUtil.getSession().set("tenantId", user.getTenantId());
        StpUtil.getSession().set("userType", user.getUserType());

        return StpUtil.getTokenValue();
    }

    @Override
    public User getByUsername(String username) {
        // 多租户环境下，需要同时匹配用户名和租户ID
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
        // 检查用户名是否已存在
        User existUser = getByUsername(user.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setUserType(1); // 普通用户
        user.setStatus(1); // 启用

        save(user);

        // 分配默认角色: user (role_id = 1)
        userRoleMapper.insertUserRole(user.getId(), 1L);
        log.info("用户注册成功，已分配默认角色: userId={}, roleId=1", user.getId());

        return user;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MiniProgramUserVO wechatLogin(WechatLoginDTO dto) {
        // TODO: 实际项目中需要调用微信API验证code并获取openid
        // 这里简化处理，使用code作为唯一标识
        String openid = "wx_" + dto.getCode();
        
        // 查询用户是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getUsername, openid)
                .eq(User::getDeleted, 0);
        User user = getOne(wrapper);
        
        // 如果用户不存在，自动注册
        if (user == null) {
            user = new User();
            user.setUsername(openid);
            user.setPassword(passwordEncoder.encode(openid)); // 使用openid作为密码
            user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : "微信用户");
            user.setAvatar(dto.getAvatar());
            user.setPhone(dto.getPhone());
            user.setUserType(1); // 普通用户
            user.setStatus(1); // 启用
            // 注意：小程序用户可能没有租户ID，或者根据业务逻辑设置默认租户
            save(user);
        } else {
            // 更新用户信息
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

        // Sa-Token 登录
        StpUtil.login(user.getId());

        // 存储用户信息到Session
        StpUtil.getSession().set("user", user);
        StpUtil.getSession().set("tenantId", user.getTenantId());
        StpUtil.getSession().set("userType", user.getUserType());

        // 获取Token
        String token = StpUtil.getTokenValue();

        // 构造返回对象
        MiniProgramUserVO userVO = new MiniProgramUserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setToken(token);

        return userVO;
    }
}

