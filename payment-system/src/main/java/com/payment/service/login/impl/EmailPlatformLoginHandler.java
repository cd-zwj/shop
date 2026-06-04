package com.payment.service.login.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.entity.PlatformUser;
import com.payment.enums.EmailCodeSceneEnum;
import com.payment.enums.PlatformLoginTypeEnum;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.EmailCodeService;
import com.payment.service.login.PlatformLoginHandler;
import com.payment.service.login.PlatformLoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 邮箱平台登录处理器，用于处理邮箱平台登录相关登录流程。
 */
@Component
@RequiredArgsConstructor
public class EmailPlatformLoginHandler implements PlatformLoginHandler {

    private final PlatformUserMapper platformUserMapper;
    private final EmailCodeService emailCodeService;

    /**
     * 判断是否支持邮箱平台登录。
     */
    @Override
    public PlatformLoginTypeEnum supports() {
        return PlatformLoginTypeEnum.EMAIL;
    }

    /**
     * 处理authenticate。
     */
    @Override
    public PlatformUser authenticate(PlatformLoginRequest request) {
        String normalizedEmail = normalizeEmail(request.principal());
        PlatformUser platformUser = platformUserMapper.selectOne(new LambdaQueryWrapper<PlatformUser>()
                .eq(PlatformUser::getEmail, normalizedEmail)
                .eq(PlatformUser::getDeleted, 0)
                .last("limit 1"));
        if (platformUser == null || platformUser.getEmailVerified() == null || platformUser.getEmailVerified() == 0) {
            throw new BusinessException("邮箱未绑定账号或未完成验证");
        }
        if (platformUser.getStatus() == null || platformUser.getStatus() == 0) {
            throw new BusinessException("用户已禁用");
        }
        emailCodeService.validateCode(normalizedEmail, request.credential(), EmailCodeSceneEnum.LOGIN, true);
        return platformUser;
    }

    /**
     * 规范化邮箱。
     */
    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
