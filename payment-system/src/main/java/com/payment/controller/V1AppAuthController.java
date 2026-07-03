package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.annotation.RateLimit;
import com.payment.common.Result;
import com.payment.config.AuthStpKit;
import com.payment.dto.AppUserVO;
import com.payment.dto.PlatformEmailSendCodeDTO;
import com.payment.dto.PlatformLoginDTO;
import com.payment.dto.PlatformRegisterDTO;
import com.payment.dto.PlatformResetPasswordDTO;
import com.payment.dto.SmsLoginDTO;
import com.payment.dto.SmsSendCodeDTO;
import com.payment.entity.PlatformUser;
import com.payment.service.AuthCaptchaService;
import com.payment.service.LoginSecurityService;
import com.payment.service.PlatformEmailAccountService;
import com.payment.service.PlatformIdentityService;
import com.payment.service.SmsCodeService;
import com.payment.service.login.PlatformLoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * C端用户认证控制器。
 * <p>
 * 提供用户注册、密码登录、短信验证码登录、第三方登录、密码重置、登出等认证接口。
 * 登录/注册等敏感操作均配置了限流策略，防止恶意刷接口。
 * <p>
 * 路径前缀：/v1/app/auth，无需登录即可访问（登出接口除外）。
 *
 * @author payment-system
 */
@RestController
@RequestMapping("/v1/app/auth")
@RequiredArgsConstructor
public class V1AppAuthController {

    private final AuthCaptchaService authCaptchaService;
    private final PlatformIdentityService platformIdentityService;
    private final LoginSecurityService loginSecurityService;
    private final SmsCodeService smsCodeService;
    private final PlatformEmailAccountService platformEmailAccountService;

    /**
     * 用户注册。
     * <p>
     * 通过邮箱/手机号+密码注册新用户，同一IP每小时最多注册5次。
     *
     * @param dto 注册信息（邮箱、密码、验证码等）
     * @return 注册成功的用户信息
     */
    @RateLimit(prefix = "auth:register", window = 3600, maxRequests = 5, includeIp = true, message = "注册过于频繁，请稍后再试")
    @PostMapping("/register")
    public Result<AppUserVO> register(@Valid @RequestBody PlatformRegisterDTO dto) {
        return Result.success(AppUserVO.toVO(platformIdentityService.register(dto)));
    }

    /**
     * 密码登录。
     * <p>
     * 通过用户名+密码+图形验证码登录，登录失败会记录失败次数并可能锁定账号。
     * 同一用户名5分钟内最多尝试10次。
     *
     * @param dto     登录信息（用户名、密码、验证码）
     * @param request HTTP请求，用于获取客户端IP地址记录登录失败
     * @return 登录成功返回JWT Token
     */
    @RateLimit(prefix = "auth:login", key = "#dto.username", window = 300, maxRequests = 10, includeIp = true, message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login/password")
    public Result<String> loginByPassword(@Valid @RequestBody PlatformLoginDTO dto,
                                          HttpServletRequest request) {
        authCaptchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        loginSecurityService.checkNotLocked(dto.getUsername());
        try {
            String token = platformIdentityService.login(
                    PlatformLoginRequest.password(dto.getUsername(), dto.getPassword()));
            loginSecurityService.clearFailures(dto.getUsername());
            return Result.success(token);
        } catch (RuntimeException e) {
            loginSecurityService.recordFailure(dto.getUsername(), request.getRemoteAddr());
            throw e;
        }
    }

    /**
     * 发送短信验证码（用于登录）。
     * <p>
     * 同一手机号60秒内只能发送一次，需先通过图形验证码校验。
     *
     * @param dto 短信发送信息（手机号、图形验证码）
     * @return 发送结果
     */
    @RateLimit(prefix = "auth:sms:send", key = "#dto.phone", window = 60, maxRequests = 3, includeIp = true, message = "验证码发送过于频繁，请稍后再试")
    @PostMapping("/sms/send-code")
    public Result<Void> sendSmsCode(@Valid @RequestBody SmsSendCodeDTO dto) {
        authCaptchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        smsCodeService.sendLoginCode(dto.getPhone());
        return Result.success();
    }

    /**
     * 发送密码重置验证码（邮箱）。
     * <p>
     * 向用户注册邮箱发送密码重置验证码，同一邮箱60秒内只能发送一次。
     *
     * @param dto 邮箱信息（邮箱地址、图形验证码）
     * @return 发送结果
     */
    @RateLimit(prefix = "auth:password-reset:send", key = "#dto.email", window = 60, maxRequests = 3, includeIp = true, message = "验证码发送过于频繁，请稍后再试")
    @PostMapping("/password/reset/send-code")
    public Result<Void> sendPasswordResetCode(@Valid @RequestBody PlatformEmailSendCodeDTO dto) {
        authCaptchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        platformEmailAccountService.sendRecoverCode(dto.getEmail());
        return Result.success();
    }

    /**
     * 重置密码。
     * <p>
     * 通过邮箱验证码验证身份后重置密码，同一邮箱5分钟内最多尝试5次。
     *
     * @param dto 密码重置信息（邮箱、邮箱验证码、新密码）
     * @return 重置结果
     */
    @RateLimit(prefix = "auth:password-reset:verify", key = "#dto.email", window = 300, maxRequests = 5, includeIp = true, message = "密码重置尝试过于频繁，请稍后再试")
    @PostMapping("/password/reset/verify")
    public Result<Void> resetPassword(@Valid @RequestBody PlatformResetPasswordDTO dto) {
        platformEmailAccountService.resetPassword(dto.getEmail(), dto.getEmailCode(), dto.getNewPassword());
        return Result.success();
    }

    /**
     * 短信验证码登录。
     * <p>
     * 通过手机号+短信验证码登录，登录失败会记录失败次数。
     * 同一手机号5分钟内最多尝试10次。
     *
     * @param dto     短信登录信息（手机号、短信验证码、图形验证码）
     * @param request HTTP请求，用于获取客户端IP地址记录登录失败
     * @return 登录成功返回JWT Token
     */
    @RateLimit(prefix = "auth:login", key = "#dto.phone", window = 300, maxRequests = 10, includeIp = true, message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login/sms")
    public Result<String> loginBySms(@Valid @RequestBody SmsLoginDTO dto,
                                     HttpServletRequest request) {
        authCaptchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        loginSecurityService.checkNotLocked(dto.getPhone());
        try {
            String token = platformIdentityService.login(
                    PlatformLoginRequest.sms(dto.getPhone(), dto.getSmsCode()));
            loginSecurityService.clearFailures(dto.getPhone());
            return Result.success(token);
        } catch (RuntimeException e) {
            loginSecurityService.recordFailure(dto.getPhone(), request.getRemoteAddr());
            throw e;
        }
    }

    /**
     * 第三方登录。
     * <p>
     * 通过第三方平台账号（如微信、支付宝等）登录，登录失败会记录失败次数。
     * 同一用户名5分钟内最多尝试10次。
     *
     * @param dto     第三方登录信息（用户名、密码/Token、验证码）
     * @param request HTTP请求，用于获取客户端IP地址记录登录失败
     * @return 登录成功返回JWT Token
     */
    @RateLimit(prefix = "auth:login", key = "#dto.username", window = 300, maxRequests = 10, includeIp = true, message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login/third-party")
    public Result<String> loginByThirdParty(@Valid @RequestBody PlatformLoginDTO dto,
                                            HttpServletRequest request) {
        authCaptchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        loginSecurityService.checkNotLocked(dto.getUsername());
        try {
            String token = platformIdentityService.login(
                    PlatformLoginRequest.thirdParty(dto.getUsername(), dto.getPassword()));
            loginSecurityService.clearFailures(dto.getUsername());
            return Result.success(token);
        } catch (RuntimeException e) {
            loginSecurityService.recordFailure(dto.getUsername(), request.getRemoteAddr());
            throw e;
        }
    }

    /**
     * 用户登出。
     * <p>
     * 清除当前用户的platform端登录会话和Token，需要已登录状态。
     *
     * @return 登出结果
     */
    @SaCheckLogin(type = "platform")
    @PostMapping("/logout")
    public Result<Void> logout() {
        AuthStpKit.PLATFORM.logout();
        return Result.success();
    }
}
