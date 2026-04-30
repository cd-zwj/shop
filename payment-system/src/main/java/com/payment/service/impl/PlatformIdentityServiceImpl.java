package com.payment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.dto.PlatformLoginDTO;
import com.payment.dto.PlatformRegisterDTO;
import com.payment.entity.PlatformUser;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.PlatformIdentityService;
import com.payment.util.BizNoGenerator;
import com.payment.util.PlatformSessionHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 平台统一用户认证服务。
 */
@Service
@RequiredArgsConstructor
public class PlatformIdentityServiceImpl implements PlatformIdentityService {

    private final PlatformUserMapper platformUserMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformUser register(PlatformRegisterDTO dto) {
        PlatformUser existUser = platformUserMapper.selectOne(new LambdaQueryWrapper<PlatformUser>()
                .eq(PlatformUser::getUsername, dto.getUsername())
                .eq(PlatformUser::getDeleted, 0));
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        PlatformUser platformUser = new PlatformUser();
        platformUser.setUserNo(BizNoGenerator.generate("PU"));
        platformUser.setUsername(dto.getUsername());
        platformUser.setPhone(dto.getPhone());
        platformUser.setEmail(dto.getEmail());
        platformUser.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        platformUser.setStatus(1);
        platformUser.setDeleted(0);
        platformUserMapper.insert(platformUser);
        platformUser.setPasswordHash(null);
        return platformUser;
    }

    @Override
    public String login(PlatformLoginDTO dto) {
        PlatformUser platformUser = platformUserMapper.selectOne(new LambdaQueryWrapper<PlatformUser>()
                .eq(PlatformUser::getUsername, dto.getUsername())
                .eq(PlatformUser::getDeleted, 0));
        if (platformUser == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (platformUser.getStatus() == null || platformUser.getStatus() == 0) {
            throw new BusinessException("用户已禁用");
        }
        if (!passwordEncoder.matches(dto.getPassword(), platformUser.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 使用带前缀的 loginId，避免和旧 sys_user 体系冲突。
        StpUtil.login("platform:" + platformUser.getId());
        StpUtil.getSession().set("platformUserId", platformUser.getId());
        StpUtil.getSession().set("platformUsername", platformUser.getUsername());
        return StpUtil.getTokenValue();
    }

    @Override
    public PlatformUser getCurrentUser() {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        PlatformUser platformUser = platformUserMapper.selectById(platformUserId);
        if (platformUser == null || platformUser.getDeleted() == 1) {
            throw new BusinessException("平台用户不存在");
        }
        platformUser.setPasswordHash(null);
        return platformUser;
    }
}
