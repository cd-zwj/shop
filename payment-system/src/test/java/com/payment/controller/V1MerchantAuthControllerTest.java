package com.payment.controller;

import com.payment.common.BusinessException;
import com.payment.common.Result;
import com.payment.dto.V1MerchantLoginDTO;
import com.payment.dto.V1MerchantSmsSendCodeDTO;
import com.payment.service.AuthCaptchaService;
import com.payment.service.PlatformIdentityService;
import com.payment.service.SmsCodeService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.service.login.PlatformLoginRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商户端认证控制器测试类，用于验证商户端认证相关逻辑。
 */
class V1MerchantAuthControllerTest {

    /**
     * 判断是否需要BuildSmsRequestForMerchant。
     */
    @Test
    void shouldBuildSmsRequestForMerchant() {
        AuthCaptchaService authCaptchaService = mock(AuthCaptchaService.class);
        PlatformIdentityService platformIdentityService = mock(PlatformIdentityService.class);
        SmsCodeService smsCodeService = mock(SmsCodeService.class);
        V1MerchantSupportService merchantSupportService = mock(V1MerchantSupportService.class);

        when(platformIdentityService.login(any())).thenThrow(new BusinessException("短信验证码错误"));

        V1MerchantAuthController controller = new V1MerchantAuthController(
                authCaptchaService,
                platformIdentityService,
                smsCodeService,
                merchantSupportService
        );

        V1MerchantLoginDTO dto = new V1MerchantLoginDTO();
        dto.setUsername("13800000000");
        dto.setPassword("654321");
        dto.setCaptchaKey("captcha-key");
        dto.setCaptchaCode("ABCD");

        BusinessException exception = assertThrows(BusinessException.class, () -> controller.loginBySms(dto));
        assertEquals("短信验证码错误", exception.getMessage());

        verify(authCaptchaService).validateCaptcha("captcha-key", "ABCD");
        ArgumentCaptor<PlatformLoginRequest> captor = ArgumentCaptor.forClass(PlatformLoginRequest.class);
        verify(platformIdentityService).login(captor.capture());
        assertEquals("13800000000", captor.getValue().principal());
        assertEquals("654321", captor.getValue().credential());
    }

    /**
     * 判断是否需要BuildThirdPartyRequestForMerchant。
     */
    @Test
    void shouldBuildThirdPartyRequestForMerchant() {
        AuthCaptchaService authCaptchaService = mock(AuthCaptchaService.class);
        PlatformIdentityService platformIdentityService = mock(PlatformIdentityService.class);
        SmsCodeService smsCodeService = mock(SmsCodeService.class);
        V1MerchantSupportService merchantSupportService = mock(V1MerchantSupportService.class);

        when(platformIdentityService.login(any())).thenThrow(new BusinessException("第三方账号未绑定平台用户"));

        V1MerchantAuthController controller = new V1MerchantAuthController(
                authCaptchaService,
                platformIdentityService,
                smsCodeService,
                merchantSupportService
        );

        V1MerchantLoginDTO dto = new V1MerchantLoginDTO();
        dto.setUsername("GITHUB");
        dto.setPassword("github-user-7");
        dto.setCaptchaKey("captcha-key");
        dto.setCaptchaCode("ABCD");

        BusinessException exception = assertThrows(BusinessException.class, () -> controller.loginByThirdParty(dto));
        assertEquals("第三方账号未绑定平台用户", exception.getMessage());

        ArgumentCaptor<PlatformLoginRequest> captor = ArgumentCaptor.forClass(PlatformLoginRequest.class);
        verify(platformIdentityService).login(captor.capture());
        assertEquals("GITHUB", captor.getValue().principal());
        assertEquals("github-user-7", captor.getValue().credential());
    }

    /**
     * 判断是否需要SendMerchantSmsCodeAfterCaptchaValidation。
     */
    @Test
    void shouldSendMerchantSmsCodeAfterCaptchaValidation() {
        AuthCaptchaService authCaptchaService = mock(AuthCaptchaService.class);
        PlatformIdentityService platformIdentityService = mock(PlatformIdentityService.class);
        SmsCodeService smsCodeService = mock(SmsCodeService.class);
        V1MerchantSupportService merchantSupportService = mock(V1MerchantSupportService.class);

        V1MerchantAuthController controller = new V1MerchantAuthController(
                authCaptchaService,
                platformIdentityService,
                smsCodeService,
                merchantSupportService
        );

        V1MerchantSmsSendCodeDTO dto = new V1MerchantSmsSendCodeDTO();
        dto.setUsername("13800000000");
        dto.setCaptchaKey("captcha-key");
        dto.setCaptchaCode("ABCD");

        Result<Void> result = controller.sendLoginSmsCode(dto);

        assertEquals(200, result.getCode());
        verify(authCaptchaService).validateCaptcha("captcha-key", "ABCD");
        verify(smsCodeService).sendLoginCode("13800000000");
    }
}
