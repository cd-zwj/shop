package com.payment.service.login.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.entity.PlatformUser;
import com.payment.enums.PlatformLoginTypeEnum;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.login.PlatformLoginHandler;
import com.payment.service.login.PlatformLoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordPlatformLoginHandler implements PlatformLoginHandler {

    private final PlatformUserMapper platformUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PlatformLoginTypeEnum supports() {
        return PlatformLoginTypeEnum.PASSWORD;
    }

    @Override
    public PlatformUser authenticate(PlatformLoginRequest request) {
        PlatformUser platformUser = platformUserMapper.selectOne(new LambdaQueryWrapper<PlatformUser>()
                .eq(PlatformUser::getUsername, request.principal())
                .eq(PlatformUser::getDeleted, 0));
        if (platformUser == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (platformUser.getStatus() == null || platformUser.getStatus() == 0) {
            throw new BusinessException("用户已禁用");
        }
        if (!passwordEncoder.matches(request.credential(), platformUser.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        return platformUser;
    }
}
