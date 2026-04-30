package com.payment.service;

import com.payment.dto.PlatformLoginDTO;
import com.payment.dto.PlatformRegisterDTO;
import com.payment.entity.PlatformUser;

public interface PlatformIdentityService {
    PlatformUser register(PlatformRegisterDTO dto);

    String login(PlatformLoginDTO dto);

    PlatformUser getCurrentUser();
}
