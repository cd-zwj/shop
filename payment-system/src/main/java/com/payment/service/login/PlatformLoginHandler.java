package com.payment.service.login;

import com.payment.entity.PlatformUser;
import com.payment.enums.PlatformLoginTypeEnum;

public interface PlatformLoginHandler {
    PlatformLoginTypeEnum supports();

    PlatformUser authenticate(PlatformLoginRequest request);
}
