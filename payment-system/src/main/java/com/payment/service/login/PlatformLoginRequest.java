package com.payment.service.login;

import com.payment.enums.PlatformLoginTypeEnum;

import java.util.Objects;

public record PlatformLoginRequest(
        PlatformLoginTypeEnum loginType,
        String principal,
        String credential
) {

    public PlatformLoginRequest {
        Objects.requireNonNull(loginType, "loginType must not be null");
    }

    public static PlatformLoginRequest password(String username, String password) {
        return new PlatformLoginRequest(PlatformLoginTypeEnum.PASSWORD, username, password);
    }

    public static PlatformLoginRequest sms(String phone, String smsCode) {
        return new PlatformLoginRequest(PlatformLoginTypeEnum.SMS, phone, smsCode);
    }

    public static PlatformLoginRequest thirdParty(String authType, String authKey) {
        return new PlatformLoginRequest(PlatformLoginTypeEnum.THIRD_PARTY, authType, authKey);
    }
}
