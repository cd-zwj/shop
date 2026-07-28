package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.entity.Store;
import com.payment.entity.TenantEmployee;
import com.payment.entity.TenantEmployeeStore;
import com.payment.mapper.StoreMapper;
import com.payment.mapper.TenantEmployeeStoreMapper;
import com.payment.service.MerchantStoreScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 统一解析商户员工的门店数据范围。 */
@Service
@RequiredArgsConstructor
public class MerchantStoreScopeService {

    private static final String ASSIGNED = "ASSIGNED";

    private final V1MerchantSupportService merchantSupportService;
    private final TenantEmployeeStoreMapper tenantEmployeeStoreMapper;
    private final StoreMapper storeMapper;

    public MerchantStoreScope resolve(Long tenantId, Long platformUserId, MerchantPermission permission) {
        TenantEmployee employee = merchantSupportService.requirePermission(tenantId, platformUserId, permission);
        String scopeType = normalize(employee.getStoreScopeType());
        if (isOwner(employee) || "ALL".equals(scopeType)) {
            return new MerchantStoreScope(tenantId, employee.getId(), true, List.of());
        }
        if (!ASSIGNED.equals(scopeType)) {
            throw new BusinessException("员工门店权限配置不合法");
        }

        List<Long> assignedStoreIds = tenantEmployeeStoreMapper.selectList(
                        new LambdaQueryWrapper<TenantEmployeeStore>()
                                .eq(TenantEmployeeStore::getTenantId, tenantId)
                                .eq(TenantEmployeeStore::getEmployeeId, employee.getId()))
                .stream()
                .map(TenantEmployeeStore::getStoreId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (assignedStoreIds.isEmpty()) {
            return new MerchantStoreScope(tenantId, employee.getId(), false, List.of());
        }

        Set<Long> validStoreIds = storeMapper.selectBatchIds(assignedStoreIds).stream()
                .filter(store -> tenantId.equals(store.getTenantId()))
                .filter(store -> !Integer.valueOf(1).equals(store.getDeleted()))
                .map(Store::getId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        List<Long> effectiveStoreIds = assignedStoreIds.stream()
                .filter(validStoreIds::contains)
                .toList();
        return new MerchantStoreScope(tenantId, employee.getId(), false, effectiveStoreIds);
    }

    public void requireStoreAccess(MerchantStoreScope scope, Long storeId) {
        if (scope == null || storeId == null || storeId <= 0) {
            throw new BusinessException("门店参数不合法");
        }
        if (!scope.allStores() && !scope.storeIds().contains(storeId)) {
            throw new BusinessException("当前员工无权访问该门店");
        }
    }

    private boolean isOwner(TenantEmployee employee) {
        return "OWNER".equals(normalize(employee.getEmployeeRole()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
