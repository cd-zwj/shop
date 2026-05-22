package com.payment.service;

import com.payment.dto.LoginCaptchaVO;

public interface AuthCaptchaService {
    LoginCaptchaVO generateCaptcha(String clientIp);

    void validateCaptcha(String captchaKey, String captchaCode);
}
