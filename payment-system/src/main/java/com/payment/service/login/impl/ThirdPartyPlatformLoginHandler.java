package com.payment.service.login.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.entity.PlatformUser;
import com.payment.entity.PlatformUserAuth;
import com.payment.enums.PlatformLoginTypeEnum;
import com.payment.mapper.PlatformUserAuthMapper;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.login.PlatformLoginHandler;
import com.payment.service.login.PlatformLoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ThirdPartyPlatformLoginHandler implements PlatformLoginHandler {

    private final PlatformUserAuthMapper platformUserAuthMapper;
    private final PlatformUserMapper platformUserMapper;

    @Override
    public PlatformLoginTypeEnum supports() {
        return PlatformLoginTypeEnum.THIRD_PARTY;
    }

    @Override
    public PlatformUser authenticate(PlatformLoginRequest request) {
        PlatformUserAuth auth = platformUserAuthMapper.selectOne(new LambdaQueryWrapper<PlatformUserAuth>()
                .eq(PlatformUserAuth::getAuthType, normalizeAuthType(request.principal()))
                .eq(PlatformUserAuth::getAuthKey, request.credential()));
        if (auth == null) {
            throw new BusinessException("第三方账号未绑定平台用户");
        }

        PlatformUser platformUser = platformUserMapper.selectById(auth.getPlatformUserId());
        if (platformUser == null || platformUser.getDeleted() == 1) {
            throw new BusinessException("平台用户不存在");
        }
        if (platformUser.getStatus() == null || platformUser.getStatus() == 0) {
            throw new BusinessException("用户已禁用");
        }
        return platformUser;
    }

    private String normalizeAuthType(String authType) {
        return authType == null ? null : authType.trim().toUpperCase(Locale.ROOT);
    }
}
