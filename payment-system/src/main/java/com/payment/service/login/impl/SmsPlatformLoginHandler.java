package com.payment.service.login.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.entity.PlatformUser;
import com.payment.enums.PlatformLoginTypeEnum;
import com.payment.mapper.PlatformUserMapper;
import com.payment.service.SmsCodeService;
import com.payment.service.login.PlatformLoginHandler;
import com.payment.service.login.PlatformLoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 短信平台登录处理器。
 */
@Component
@RequiredArgsConstructor
public class SmsPlatformLoginHandler implements PlatformLoginHandler {

    private final SmsCodeService smsCodeService;
    private final PlatformUserMapper platformUserMapper;

    @Override
    public PlatformLoginTypeEnum supports() {
        return PlatformLoginTypeEnum.SMS;
    }

    @Override
    public PlatformUser authenticate(PlatformLoginRequest request) {
        String phone = request.principal();
        String smsCode = request.credential();

        smsCodeService.validateLoginCode(phone, smsCode, true);

        PlatformUser platformUser = platformUserMapper.selectOne(new LambdaQueryWrapper<PlatformUser>()
                .eq(PlatformUser::getPhone, phone)
                .eq(PlatformUser::getDeleted, 0));

        if (platformUser == null) {
            throw new BusinessException("该手机号未注册");
        }
        if (platformUser.getStatus() == null || platformUser.getStatus() == 0) {
            throw new BusinessException("用户已禁用");
        }

        return platformUser;
    }
}
