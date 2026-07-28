package com.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.payment.common.Result;
import com.payment.dto.V1MerchantEmployeeCreateDTO;
import com.payment.dto.V1MerchantEmployeeRoleUpdateDTO;
import com.payment.dto.V1MerchantEmployeeStatusUpdateDTO;
import com.payment.dto.V1MerchantEmployeeStoreScopeUpdateDTO;
import com.payment.dto.V1MerchantEmployeeVO;
import com.payment.service.V1MerchantEmployeeService;
import com.payment.util.PlatformSessionHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商户端员工与本地角色管理控制器。
 */
@RestController
@RequestMapping("/v1/merchant/tenants/{tenantId}/employees")
@RequiredArgsConstructor
public class V1MerchantEmployeeController {

    private final V1MerchantEmployeeService employeeService;

    @SaCheckLogin(type = "merchant")
    @GetMapping
    public Result<List<V1MerchantEmployeeVO>> listEmployees(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId) {
        return Result.success(employeeService.listEmployees(tenantId, PlatformSessionHelper.getPlatformUserId()));
    }

    @SaCheckLogin(type = "merchant")
    @PostMapping
    public Result<V1MerchantEmployeeVO> addEmployee(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @Valid @RequestBody V1MerchantEmployeeCreateDTO dto) {
        return Result.success(employeeService.addEmployee(tenantId, PlatformSessionHelper.getPlatformUserId(), dto));
    }

    @SaCheckLogin(type = "merchant")
    @PutMapping("/{employeeId}/role")
    public Result<V1MerchantEmployeeVO> updateRole(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long employeeId,
            @Valid @RequestBody V1MerchantEmployeeRoleUpdateDTO dto) {
        return Result.success(employeeService.updateRole(
                tenantId, PlatformSessionHelper.getPlatformUserId(), employeeId, dto.getEmployeeRole()));
    }

    @SaCheckLogin(type = "merchant")
    @PutMapping("/{employeeId}/status")
    public Result<V1MerchantEmployeeVO> updateStatus(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long employeeId,
            @Valid @RequestBody V1MerchantEmployeeStatusUpdateDTO dto) {
        return Result.success(employeeService.updateStatus(
                tenantId, PlatformSessionHelper.getPlatformUserId(), employeeId, dto.getStatus()));
    }

    @SaCheckLogin(type = "merchant")
    @PutMapping("/{employeeId}/store-scope")
    public Result<V1MerchantEmployeeVO> updateStoreScope(
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long tenantId,
            @PathVariable @Min(value = 1, message = "ID必须大于0") Long employeeId,
            @Valid @RequestBody V1MerchantEmployeeStoreScopeUpdateDTO dto) {
        return Result.success(employeeService.updateStoreScope(
                tenantId, PlatformSessionHelper.getPlatformUserId(), employeeId, dto));
    }
}
