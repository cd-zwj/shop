package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.dto.V1MerchantEmployeeCreateDTO;
import com.payment.dto.V1MerchantEmployeeVO;
import com.payment.dto.V1MerchantEmployeeStoreScopeUpdateDTO;
import com.payment.entity.PlatformUser;
import com.payment.entity.Store;
import com.payment.entity.TenantEmployee;
import com.payment.entity.TenantEmployeeStore;
import com.payment.mapper.PlatformUserMapper;
import com.payment.mapper.StoreMapper;
import com.payment.mapper.TenantEmployeeMapper;
import com.payment.mapper.TenantEmployeeStoreMapper;
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
            "OWNER", "ADMIN", "MANAGER", "OPERATOR", "PICKUP_CLERK", "FINANCE");
    private static final Set<String> MANAGEMENT_ROLES = Set.of("OWNER", "ADMIN");
    private static final Set<String> STORE_SCOPE_TYPES = Set.of("ALL", "ASSIGNED");

    private final TenantEmployeeMapper tenantEmployeeMapper;
    private final PlatformUserMapper platformUserMapper;
    private final TenantEmployeeStoreMapper tenantEmployeeStoreMapper;
    private final StoreMapper storeMapper;
    private final V1MerchantSupportService v1MerchantSupportService;

    @Override
    public List<V1MerchantEmployeeVO> listEmployees(Long tenantId, Long operatorPlatformUserId) {
        requireEmployeeManagementPermission(tenantId, operatorPlatformUserId);
        List<TenantEmployee> employees = tenantEmployeeMapper.selectList(new LambdaQueryWrapper<TenantEmployee>()
                .eq(TenantEmployee::getTenantId, tenantId)
                .orderByDesc(TenantEmployee::getStatus)
                .orderByAsc(TenantEmployee::getCreateTime));
        Map<Long, PlatformUser> users = loadUsers(employees);
        Map<Long, List<Long>> storeIdsByEmployee = loadStoreIds(tenantId, employees);
        return employees.stream()
                .map(employee -> toVO(employee, users.get(employee.getPlatformUserId()), storeIdsByEmployee))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantEmployeeVO addEmployee(Long tenantId, Long operatorPlatformUserId, V1MerchantEmployeeCreateDTO dto) {
        TenantEmployee operator = requireEmployeeManagementPermission(tenantId, operatorPlatformUserId);
        String role = normalizeRole(dto.getEmployeeRole());
        ensureCanAssignRole(operator, role);
        String storeScopeType = normalizeStoreScopeType(dto.getStoreScopeType(), role);
        ensureCanAssignScope(operator, role, storeScopeType);
        List<Long> storeIds = validateStoreIds(tenantId, storeScopeType, dto.getStoreIds());
        PlatformUser user = requireActivePlatformUser(dto.getPlatformUserId());

        TenantEmployee existing = tenantEmployeeMapper.selectOne(new LambdaQueryWrapper<TenantEmployee>()
                .eq(TenantEmployee::getTenantId, tenantId)
                .eq(TenantEmployee::getPlatformUserId, dto.getPlatformUserId()));
        if (existing != null) {
            ensureCanManageTarget(operator, existing);
        }
        if (existing != null && Integer.valueOf(1).equals(existing.getStatus())) {
            throw new BusinessException("该用户已经是当前商户的启用员工");
        }

        LocalDateTime now = LocalDateTime.now();
        TenantEmployee employee = existing == null ? new TenantEmployee() : existing;
        employee.setTenantId(tenantId);
        employee.setPlatformUserId(dto.getPlatformUserId());
        employee.setEmployeeRole(role);
        employee.setStoreScopeType(storeScopeType);
        employee.setStatus(1);
        employee.setUpdateTime(now);
        if (existing == null) {
            employee.setEmployeeNo(BizNoGenerator.generate("EMP"));
            employee.setCreateTime(now);
            tenantEmployeeMapper.insert(employee);
        } else {
            tenantEmployeeMapper.updateById(employee);
        }
        replaceStoreAssignments(tenantId, employee.getId(), operatorPlatformUserId, storeIds);
        return V1MerchantEmployeeVO.from(employee, user, storeIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantEmployeeVO updateRole(Long tenantId, Long operatorPlatformUserId, Long employeeId, String employeeRole) {
        TenantEmployee operator = requireEmployeeManagementPermission(tenantId, operatorPlatformUserId);
        TenantEmployee employee = requireTenantEmployee(tenantId, employeeId);
        String previousRole = normalizeRole(employee.getEmployeeRole());
        String nextRole = normalizeRole(employeeRole);
        ensureCanManageTarget(operator, employee);
        ensureCanAssignRole(operator, nextRole);

        if (Objects.equals(employee.getPlatformUserId(), operator.getPlatformUserId())
                && !MANAGEMENT_ROLES.contains(nextRole)) {
            throw new BusinessException("不能将自己的角色调整为无员工管理权限");
        }
        ensureOwnerInvariantWhenRoleChanges(tenantId, employee, nextRole);

        employee.setEmployeeRole(nextRole);
        if ("OWNER".equals(nextRole)) {
            employee.setStoreScopeType("ALL");
            replaceStoreAssignments(tenantId, employeeId, operatorPlatformUserId, List.of());
        } else if ("OWNER".equals(previousRole)) {
            employee.setStoreScopeType("ASSIGNED");
            replaceStoreAssignments(tenantId, employeeId, operatorPlatformUserId, List.of());
        }
        employee.setUpdateTime(LocalDateTime.now());
        tenantEmployeeMapper.updateById(employee);
        return toVO(employee, platformUserMapper.selectById(employee.getPlatformUserId()),
                loadStoreIds(tenantId, List.of(employee)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantEmployeeVO updateStatus(Long tenantId, Long operatorPlatformUserId, Long employeeId, Integer status) {
        TenantEmployee operator = requireEmployeeManagementPermission(tenantId, operatorPlatformUserId);
        TenantEmployee employee = requireTenantEmployee(tenantId, employeeId);
        ensureCanManageTarget(operator, employee);
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
        return toVO(employee, platformUserMapper.selectById(employee.getPlatformUserId()),
                loadStoreIds(tenantId, List.of(employee)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public V1MerchantEmployeeVO updateStoreScope(Long tenantId, Long operatorPlatformUserId, Long employeeId,
                                                  V1MerchantEmployeeStoreScopeUpdateDTO dto) {
        TenantEmployee operator = requireEmployeeManagementPermission(tenantId, operatorPlatformUserId);
        TenantEmployee employee = requireTenantEmployee(tenantId, employeeId);
        ensureCanManageTarget(operator, employee);
        String role = normalizeRole(employee.getEmployeeRole());
        String scopeType = normalizeStoreScopeType(dto.getStoreScopeType(), role);
        ensureCanAssignScope(operator, role, scopeType);
        List<Long> storeIds = validateStoreIds(tenantId, scopeType, dto.getStoreIds());

        employee.setStoreScopeType(scopeType);
        employee.setUpdateTime(LocalDateTime.now());
        tenantEmployeeMapper.updateById(employee);
        replaceStoreAssignments(tenantId, employeeId, operatorPlatformUserId, storeIds);
        return V1MerchantEmployeeVO.from(
                employee, platformUserMapper.selectById(employee.getPlatformUserId()), storeIds);
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

    private String normalizeStoreScopeType(String rawScopeType, String role) {
        if ("OWNER".equals(role)) {
            if (rawScopeType != null && !"ALL".equals(normalize(rawScopeType))) {
                throw new BusinessException("OWNER 必须具有全部门店权限");
            }
            return "ALL";
        }
        String scopeType = rawScopeType == null ? "ASSIGNED" : normalize(rawScopeType);
        if (!STORE_SCOPE_TYPES.contains(scopeType)) {
            throw new BusinessException("门店范围类型只能为 ALL 或 ASSIGNED");
        }
        return scopeType;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void ensureCanAssignRole(TenantEmployee operator, String role) {
        if ("OWNER".equals(role) && !"OWNER".equals(normalizeRole(operator.getEmployeeRole()))) {
            throw new BusinessException("仅 OWNER 可授予 OWNER 角色");
        }
    }

    private void ensureCanAssignScope(TenantEmployee operator, String role, String scopeType) {
        if ("OWNER".equals(role) && !"ALL".equals(scopeType)) {
            throw new BusinessException("OWNER 必须具有全部门店权限");
        }
        if ("ALL".equals(scopeType) && !"OWNER".equals(normalizeRole(operator.getEmployeeRole()))) {
            throw new BusinessException("仅 OWNER 可授予全部门店权限");
        }
    }

    private void ensureCanManageTarget(TenantEmployee operator, TenantEmployee target) {
        if ("OWNER".equals(normalizeRole(target.getEmployeeRole()))
                && !"OWNER".equals(normalizeRole(operator.getEmployeeRole()))) {
            throw new BusinessException("仅 OWNER 可管理 OWNER 账号");
        }
    }

    private List<Long> validateStoreIds(Long tenantId, String scopeType, List<Long> requestedStoreIds) {
        if ("ALL".equals(scopeType)) {
            if (requestedStoreIds != null && !requestedStoreIds.isEmpty()) {
                throw new BusinessException("全部门店范围无需指定门店");
            }
            return List.of();
        }
        if (requestedStoreIds != null && (requestedStoreIds.size() > 100
                || requestedStoreIds.stream().anyMatch(id -> id == null || id <= 0))) {
            throw new BusinessException("门店分配参数不合法");
        }
        List<Long> storeIds = requestedStoreIds == null ? List.of() : requestedStoreIds.stream()
                .distinct()
                .sorted()
                .toList();
        if (storeIds.isEmpty()) {
            return List.of();
        }
        Set<Long> validStoreIds = storeMapper.selectBatchIds(storeIds).stream()
                .filter(store -> tenantId.equals(store.getTenantId()))
                .filter(store -> !Integer.valueOf(1).equals(store.getDeleted()))
                .map(Store::getId)
                .collect(Collectors.toSet());
        if (validStoreIds.size() != storeIds.size()) {
            throw new BusinessException("存在不属于当前商户的门店");
        }
        return storeIds;
    }

    private void replaceStoreAssignments(Long tenantId, Long employeeId, Long operatorPlatformUserId,
                                         List<Long> storeIds) {
        tenantEmployeeStoreMapper.delete(new LambdaQueryWrapper<TenantEmployeeStore>()
                .eq(TenantEmployeeStore::getTenantId, tenantId)
                .eq(TenantEmployeeStore::getEmployeeId, employeeId));
        LocalDateTime now = LocalDateTime.now();
        for (Long storeId : storeIds) {
            TenantEmployeeStore assignment = new TenantEmployeeStore();
            assignment.setTenantId(tenantId);
            assignment.setEmployeeId(employeeId);
            assignment.setStoreId(storeId);
            assignment.setCreatedBy(operatorPlatformUserId);
            assignment.setCreateTime(now);
            tenantEmployeeStoreMapper.insert(assignment);
        }
    }

    private Map<Long, List<Long>> loadStoreIds(Long tenantId, List<TenantEmployee> employees) {
        Set<Long> employeeIds = employees.stream()
                .map(TenantEmployee::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        return tenantEmployeeStoreMapper.selectList(new LambdaQueryWrapper<TenantEmployeeStore>()
                        .eq(TenantEmployeeStore::getTenantId, tenantId)
                        .in(TenantEmployeeStore::getEmployeeId, employeeIds))
                .stream()
                .collect(Collectors.groupingBy(
                        TenantEmployeeStore::getEmployeeId,
                        Collectors.mapping(TenantEmployeeStore::getStoreId, Collectors.toList())));
    }

    private V1MerchantEmployeeVO toVO(TenantEmployee employee, PlatformUser user,
                                      Map<Long, List<Long>> storeIdsByEmployee) {
        return V1MerchantEmployeeVO.from(
                employee, user, storeIdsByEmployee.getOrDefault(employee.getId(), List.of()));
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
        return tenantEmployeeMapper.selectActiveOwnersForUpdate(tenantId).stream()
                .filter(owner -> !Objects.equals(owner.getId(), employeeId))
                .count();
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
