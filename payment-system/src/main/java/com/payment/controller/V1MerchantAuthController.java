package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.payment.common.BusinessException;
import com.payment.common.Result;
import com.payment.dto.V1MerchantLoginDTO;
import com.payment.dto.V1MerchantSessionVO;
import com.payment.dto.V1MerchantTenantVO;
import com.payment.entity.PlatformUser;
import com.payment.entity.TenantEmployee;
import com.payment.service.PlatformIdentityService;
import com.payment.service.impl.V1MerchantSupportService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/merchant/auth")
@RequiredArgsConstructor
public class V1MerchantAuthController {

    private final PlatformIdentityService platformIdentityService;
    private final V1MerchantSupportService v1MerchantSupportService;

    @PostMapping("/login")
    public Result<V1MerchantSessionVO> login(@Valid @RequestBody V1MerchantLoginDTO dto) {
        String token = platformIdentityService.login(toPlatformLogin(dto));
        Long platformUserId = PlatformSessionHelper.getPlatformUserId();
        PlatformUser currentUser = platformIdentityService.getCurrentUser();

        List<TenantEmployee> employees = v1MerchantSupportService.listActiveEmployees(platformUserId);
        if (employees.isEmpty()) {
            StpUtil.logout();
            throw new BusinessException("当前账号不是有效的商家员工账号");
        }

        List<V1MerchantTenantVO> tenants = v1MerchantSupportService.listAccessibleTenants(platformUserId);
        V1MerchantTenantVO currentTenant = tenants.get(0);
        StpUtil.getSession().set("merchantTenantId", currentTenant.getTenantId());
        StpUtil.getSession().set("merchantEmployeeRole", currentTenant.getEmployeeRole());

        V1MerchantSessionVO vo = new V1MerchantSessionVO();
        vo.setToken(token);
        vo.setExpiresIn(StpUtil.getTokenTimeout());
        vo.setPlatformUserId(currentUser.getId());
        vo.setUsername(currentUser.getUsername());
        vo.setTenantId(currentTenant.getTenantId());
        vo.setTenantName(currentTenant.getTenantName());
        vo.setEmployeeRole(currentTenant.getEmployeeRole());
        vo.setTenants(tenants);
        return Result.success(vo);
    }

    @SaCheckLogin
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
        vo.setToken(StpUtil.getTokenValue());
        vo.setExpiresIn(StpUtil.getTokenTimeout());
        vo.setPlatformUserId(currentUser.getId());
        vo.setUsername(currentUser.getUsername());
        vo.setTenantId(currentTenant.getTenantId());
        vo.setTenantName(currentTenant.getTenantName());
        vo.setEmployeeRole(currentTenant.getEmployeeRole());
        vo.setTenants(tenants);
        return Result.success(vo);
    }

    @SaCheckLogin
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }

    private com.payment.dto.PlatformLoginDTO toPlatformLogin(V1MerchantLoginDTO dto) {
        com.payment.dto.PlatformLoginDTO loginDTO = new com.payment.dto.PlatformLoginDTO();
        loginDTO.setUsername(dto.getUsername());
        loginDTO.setPassword(dto.getPassword());
        return loginDTO;
    }

    private Long sessionLong(String key) {
        Object value = StpUtil.getSession().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
