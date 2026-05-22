package com.payment.service.login.impl;

import com.payment.common.BusinessException;
import com.payment.entity.PlatformUser;
import com.payment.enums.PlatformLoginTypeEnum;
import com.payment.service.login.PlatformLoginHandler;
import com.payment.service.login.PlatformLoginRequest;
import org.springframework.stereotype.Component;

@Component
public class SmsPlatformLoginHandler implements PlatformLoginHandler {

    @Override
    public PlatformLoginTypeEnum supports() {
        return PlatformLoginTypeEnum.SMS;
    }

    @Override
    public PlatformUser authenticate(PlatformLoginRequest request) {
        throw new BusinessException("短信登录暂未开通");
    }
}
