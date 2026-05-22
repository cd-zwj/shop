package com.payment.service;

import com.payment.dto.PlatformRegisterDTO;
import com.payment.entity.PlatformUser;
import com.payment.service.login.PlatformLoginRequest;

public interface PlatformIdentityService {
    PlatformUser register(PlatformRegisterDTO dto);

    String login(PlatformLoginRequest request);

    PlatformUser getCurrentUser();
}
