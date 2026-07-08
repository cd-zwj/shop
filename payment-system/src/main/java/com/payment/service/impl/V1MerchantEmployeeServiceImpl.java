package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.dto.V1MerchantEmployeeCreateDTO;
import com.payment.dto.V1MerchantEmployeeVO;
import com.payment.entity.PlatformUser;
import com.payment.entity.TenantEmployee;
import com.payment.mapper.PlatformUserMapper;
import com.payment.mapper.TenantEmployeeMapper;
import com.payment.service.V1MerchantEmployeeService;
import com.payment.util.BizNoGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商户端员工与本地角色管理实现。
 */
@Service
@RequiredArgsConstructor
public class V1MerchantEmployeeServiceImpl implements V1MerchantEmployeeService {

    private static final Set<String> SUPPORTED_ROLES = Set.of(
            "OWNER", "ADMIN", "MANAGER", "OPERATOR", "CASHIER", "FINANCE");
    private static final Set<String> MANAGEMENT_ROLES = Set.of("OWNER", "ADMIN");

    private final TenantEmployeeMapper tenantEmployeeMapper;
    private final PlatformUserMapper platformUserMapper;
    private final V1MerchantSupportService v1MerchantSupportService;

    @Override
    public List<V1MerchantEmployeeVO> listEmployees(Long tenantId, Long operatorPlatformUserId) {
        requireEmployeeManagementPermission(tenantId, operatorPlatformUserId);
        List<TenantEmployee> employees = tenantEmployeeMapper.selectList(new LambdaQueryWrapper<TenantEmployee>()
                .eq(TenantEmployee::getTenantId, tenantId)
                .orderByDesc(TenantEmployee::getStatus)
                .orderByAsc(TenantEmployee::getCreateTime));
        Map<Long, PlatformUser> users = loadUsers(employees);
        return employees.stream()
                .map(employee -> V1MerchantEmployeeVO.from(employee, users.get(employee.getPlatformUserId())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantEmployeeVO addEmployee(Long tenantId, Long operatorPlatformUserId, V1MerchantEmployeeCreateDTO dto) {
        requireEmployeeManagementPermission(tenantId, operatorPlatformUserId);
        String role = normalizeRole(dto.getEmployeeRole());
        PlatformUser user = requireActivePlatformUser(dto.getPlatformUserId());

        TenantEmployee existing = tenantEmployeeMapper.selectOne(new LambdaQueryWrapper<TenantEmployee>()
                .eq(TenantEmployee::getTenantId, tenantId)
                .eq(TenantEmployee::getPlatformUserId, dto.getPlatformUserId()));
        if (existing != null && Integer.valueOf(1).equals(existing.getStatus())) {
            throw new BusinessException("该用户已经是当前商户的启用员工");
        }

        LocalDateTime now = LocalDateTime.now();
        TenantEmployee employee = existing == null ? new TenantEmployee() : existing;
        employee.setTenantId(tenantId);
        employee.setPlatformUserId(dto.getPlatformUserId());
        employee.setEmployeeRole(role);
        employee.setStatus(1);
        employee.setUpdateTime(now);
        if (existing == null) {
            employee.setEmployeeNo(BizNoGenerator.generate("EMP"));
            employee.setCreateTime(now);
            tenantEmployeeMapper.insert(employee);
        } else {
            tenantEmployeeMapper.updateById(employee);
        }
        return V1MerchantEmployeeVO.from(employee, user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantEmployeeVO updateRole(Long tenantId, Long operatorPlatformUserId, Long employeeId, String employeeRole) {
        TenantEmployee operator = requireEmployeeManagementPermission(tenantId, operatorPlatformUserId);
        TenantEmployee employee = requireTenantEmployee(tenantId, employeeId);
        String nextRole = normalizeRole(employeeRole);

        if (Objects.equals(employee.getPlatformUserId(), operator.getPlatformUserId())
                && !MANAGEMENT_ROLES.contains(nextRole)) {
            throw new BusinessException("不能将自己的角色调整为无员工管理权限");
        }
        ensureOwnerInvariantWhenRoleChanges(tenantId, employee, nextRole);

        employee.setEmployeeRole(nextRole);
        employee.setUpdateTime(LocalDateTime.now());
        tenantEmployeeMapper.updateById(employee);
        return V1MerchantEmployeeVO.from(employee, platformUserMapper.selectById(employee.getPlatformUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantEmployeeVO updateStatus(Long tenantId, Long operatorPlatformUserId, Long employeeId, Integer status) {
        TenantEmployee operator = requireEmployeeManagementPermission(tenantId, operatorPlatformUserId);
        TenantEmployee employee = requireTenantEmployee(tenantId, employeeId);
        Integer nextStatus = status == null ? null : status;
        if (nextStatus == null || (nextStatus != 0 && nextStatus != 1)) {
            throw new BusinessException("员工状态只能为0或1");
        }

        if (Objects.equals(employee.getPlatformUserId(), operator.getPlatformUserId()) && nextStatus == 0) {
            throw new BusinessException("不能禁用自己的商户员工账号");
        }
        ensureOwnerInvariantWhenStatusChanges(tenantId, employee, nextStatus);

        employee.setStatus(nextStatus);
        employee.setUpdateTime(LocalDateTime.now());
        tenantEmployeeMapper.updateById(employee);
        return V1MerchantEmployeeVO.from(employee, platformUserMapper.selectById(employee.getPlatformUserId()));
    }

    private TenantEmployee requireEmployeeManagementPermission(Long tenantId, Long platformUserId) {
        return v1MerchantSupportService.requirePermission(tenantId, platformUserId, MerchantPermission.EMPLOYEE_MANAGE);
    }

    private TenantEmployee requireTenantEmployee(Long tenantId, Long employeeId) {
        TenantEmployee employee = tenantEmployeeMapper.selectOne(new LambdaQueryWrapper<TenantEmployee>()
                .eq(TenantEmployee::getTenantId, tenantId)
                .eq(TenantEmployee::getId, employeeId));
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        return employee;
    }

    private PlatformUser requireActivePlatformUser(Long platformUserId) {
        PlatformUser user = platformUserMapper.selectById(platformUserId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted()) || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException("平台用户不存在或已停用");
        }
        return user;
    }

    private String normalizeRole(String rawRole) {
        String role = rawRole == null ? "" : rawRole.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_ROLES.contains(role)) {
            throw new BusinessException("不支持的员工角色: " + rawRole);
        }
        return role;
    }

    private void ensureOwnerInvariantWhenRoleChanges(Long tenantId, TenantEmployee employee, String nextRole) {
        if (!"OWNER".equals(normalizeRole(employee.getEmployeeRole())) || "OWNER".equals(nextRole)) {
            return;
        }
        if (countOtherActiveOwners(tenantId, employee.getId()) <= 0) {
            throw new BusinessException("不能移除最后一个启用 OWNER");
        }
    }

    private void ensureOwnerInvariantWhenStatusChanges(Long tenantId, TenantEmployee employee, Integer nextStatus) {
        if (!"OWNER".equals(normalizeRole(employee.getEmployeeRole())) || !Integer.valueOf(1).equals(employee.getStatus()) || nextStatus == 1) {
            return;
        }
        if (countOtherActiveOwners(tenantId, employee.getId()) <= 0) {
            throw new BusinessException("不能禁用最后一个启用 OWNER");
        }
    }

    private long countOtherActiveOwners(Long tenantId, Long employeeId) {
        Long count = tenantEmployeeMapper.selectCount(new LambdaQueryWrapper<TenantEmployee>()
                .eq(TenantEmployee::getTenantId, tenantId)
                .eq(TenantEmployee::getEmployeeRole, "OWNER")
                .eq(TenantEmployee::getStatus, 1)
                .ne(TenantEmployee::getId, employeeId));
        return count == null ? 0 : count;
    }

    private Map<Long, PlatformUser> loadUsers(List<TenantEmployee> employees) {
        List<Long> userIds = employees.stream()
                .map(TenantEmployee::getPlatformUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return platformUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(PlatformUser::getId, Function.identity(), (left, right) -> left));
    }
}
