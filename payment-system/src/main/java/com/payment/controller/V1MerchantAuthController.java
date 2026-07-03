package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.annotation.RateLimit;
import com.payment.common.BusinessException;
import com.payment.common.Result;
import com.payment.config.AuthStpKit;
import com.payment.dto.V1MerchantLoginDTO;
import com.payment.dto.V1MerchantSessionVO;
import com.payment.dto.V1MerchantTenantVO;
import com.payment.entity.PlatformUser;
import com.payment.entity.TenantEmployee;
import com.payment.service.AuthCaptchaService;
import com.payment.service.PlatformIdentityService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.service.login.PlatformLoginRequest;
import com.payment.util.AuthLoginIdHelper;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商户端认证控制器（Merchant 端）。
 * <p>提供商户员工的登录、获取当前会话信息、登出等认证接口，
 * 基于 Sa-Token MERCHANT 体系实现会话管理。</p>
 */
@RestController
@RequestMapping("/v1/merchant/auth")
@RequiredArgsConstructor
public class V1MerchantAuthController {

    private final AuthCaptchaService authCaptchaService;
    private final PlatformIdentityService platformIdentityService;
    private final V1MerchantSupportService v1MerchantSupportService;

    /**
     * 商户员工登录。
     * <p>验证图形验证码后，通过用户名密码认证平台用户身份，
     * 检查是否为有效商户员工，创建 Sa-Token MERCHANT 会话并返回 Token 及商户租户列表。</p>
     *
     * @param dto 登录请求参数（用户名、密码、验证码）
     * @return 包含 Token、用户信息、当前租户及可访问租户列表的会话信息
     */
    @RateLimit(prefix = "auth:login", key = "#dto.username", window = 300, maxRequests = 10, includeIp = true, message = "登录尝试过于频繁，请稍后再试")
    @PostMapping("/login")
    public Result<V1MerchantSessionVO> login(@Valid @RequestBody V1MerchantLoginDTO dto) {
        authCaptchaService.validateCaptcha(dto.getCaptchaKey(), dto.getCaptchaCode());
        PlatformUser currentUser = platformIdentityService.authenticate(PlatformLoginRequest.password(dto.getUsername(), dto.getPassword()));
        Long platformUserId = currentUser.getId();

        List<TenantEmployee> employees = v1MerchantSupportService.listActiveEmployees(platformUserId);
        if (employees.isEmpty()) {
            throw new BusinessException("当前账号不是有效的商家员工账号");
        }

        List<V1MerchantTenantVO> tenants = v1MerchantSupportService.listAccessibleTenants(platformUserId);
        V1MerchantTenantVO currentTenant = tenants.get(0);
        AuthStpKit.MERCHANT.login(AuthLoginIdHelper.merchant(platformUserId));
        AuthStpKit.MERCHANT.getSession().set("platformUserId", currentUser.getId());
        AuthStpKit.MERCHANT.getSession().set("platformUsername", currentUser.getUsername());
        AuthStpKit.MERCHANT.getSession().set("merchantTenantId", currentTenant.getTenantId());
        AuthStpKit.MERCHANT.getSession().set("merchantEmployeeRole", currentTenant.getEmployeeRole());

        V1MerchantSessionVO vo = new V1MerchantSessionVO();
        vo.setToken(AuthStpKit.MERCHANT.getTokenValue());
        vo.setExpiresIn(AuthStpKit.MERCHANT.getTokenTimeout());
        vo.setPlatformUserId(currentUser.getId());
        vo.setUsername(currentUser.getUsername());
        vo.setTenantId(currentTenant.getTenantId());
        vo.setTenantName(currentTenant.getTenantName());
        vo.setEmployeeRole(currentTenant.getEmployeeRole());
        vo.setTenants(tenants);
        return Result.success(vo);
    }

    /**
     * 获取当前登录商户员工的会话信息。
     * <p>根据当前会话中的平台用户 ID 刷新可访问的租户列表，
     * 并返回包含 Token 有效期、用户信息和租户列表的完整会话信息。</p>
     *
     * @return 当前会话的完整信息，包括 Token、用户信息及可访问租户列表
     */
    @SaCheckLogin(type = AuthStpKit.MERCHANT_TYPE)
    @GetMapping("/me")
    public Result<V1MerchantSessionVO> me() {
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        PlatformUser currentUser = platformIdentityService.getCurrentUser();
        List<V1MerchantTenantVO> tenants = v1MerchantSupportService.listAccessibleTenants(platformUserId);
        if (tenants.isEmpty()) {
            throw new BusinessException("当前账号没有可用的商家身份");
        }

        Long tenantId = sessionLong("merchantTenantId");
        V1MerchantTenantVO currentTenant = tenants.stream()
                .filter(item -> item.getTenantId().equals(tenantId))
                .findFirst()
                .orElse(tenants.get(0));

        V1MerchantSessionVO vo = new V1MerchantSessionVO();
        vo.setToken(AuthStpKit.MERCHANT.getTokenValue());
        vo.setExpiresIn(AuthStpKit.MERCHANT.getTokenTimeout());
        vo.setPlatformUserId(currentUser.getId());
        vo.setUsername(currentUser.getUsername());
        vo.setTenantId(currentTenant.getTenantId());
        vo.setTenantName(currentTenant.getTenantName());
        vo.setEmployeeRole(currentTenant.getEmployeeRole());
        vo.setTenants(tenants);
        return Result.success(vo);
    }

    /**
     * 商户员工登出。
     * <p>注销当前 Sa-Token MERCHANT 会话，清除服务端会话数据。</p>
     *
     * @return 操作结果
     */
    @SaCheckLogin(type = AuthStpKit.MERCHANT_TYPE)
    @PostMapping("/logout")
    public Result<Void> logout() {
        AuthStpKit.MERCHANT.logout();
        return Result.success();
    }

    private Long sessionLong(String key) {
        Object value = AuthStpKit.MERCHANT.getSession().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
