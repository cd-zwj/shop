package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.entity.Store;
import com.payment.entity.TenantEmployee;
import com.payment.entity.TenantEmployeeStore;
import com.payment.mapper.StoreMapper;
import com.payment.mapper.TenantEmployeeStoreMapper;
import com.payment.service.MerchantStoreScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MerchantStoreScopeServiceTest {

    @Test
    void ownerShouldAlwaysReceiveAllStoreAccess() {
        Fixture fixture = new Fixture();
        TenantEmployee employee = employee("OWNER", "ASSIGNED");
        when(fixture.supportService.requirePermission(9L, 100L, MerchantPermission.ORDER_MANAGE))
                .thenReturn(employee);

        MerchantStoreScope scope = fixture.service.resolve(9L, 100L, MerchantPermission.ORDER_MANAGE);

        assertTrue(scope.allStores());
        assertTrue(scope.storeIds().isEmpty());
    }

    @Test
    void assignedEmployeeShouldOnlyReceiveValidTenantStores() {
        Fixture fixture = new Fixture();
        TenantEmployee employee = employee("MANAGER", "ASSIGNED");
        when(fixture.supportService.requirePermission(9L, 100L, MerchantPermission.INVENTORY_MANAGE))
                .thenReturn(employee);
        when(fixture.employeeStoreMapper.selectList(any())).thenReturn(List.of(assignment(7L), assignment(8L)));
        when(fixture.storeMapper.selectBatchIds(List.of(7L, 8L)))
                .thenReturn(List.of(store(7L, 9L, 0), store(8L, 10L, 0)));

        MerchantStoreScope scope = fixture.service.resolve(9L, 100L, MerchantPermission.INVENTORY_MANAGE);

        assertFalse(scope.allStores());
        assertEquals(List.of(7L), scope.storeIds());
        fixture.service.requireStoreAccess(scope, 7L);
        assertThrows(BusinessException.class, () -> fixture.service.requireStoreAccess(scope, 8L));
    }

    @Test
    void assignedEmployeeWithoutStoresShouldHaveEmptyScope() {
        Fixture fixture = new Fixture();
        when(fixture.supportService.requirePermission(9L, 100L, MerchantPermission.ORDER_MANAGE))
                .thenReturn(employee("PICKUP_CLERK", "ASSIGNED"));
        when(fixture.employeeStoreMapper.selectList(any())).thenReturn(List.of());

        MerchantStoreScope scope = fixture.service.resolve(9L, 100L, MerchantPermission.ORDER_MANAGE);

        assertFalse(scope.allStores());
        assertTrue(scope.storeIds().isEmpty());
        assertThrows(BusinessException.class, () -> fixture.service.requireStoreAccess(scope, 7L));
    }

    @Test
    void nonOwnerWithBlankScopeConfigurationShouldFailClosed() {
        Fixture fixture = new Fixture();
        when(fixture.supportService.requirePermission(9L, 100L, MerchantPermission.ORDER_MANAGE))
                .thenReturn(employee("MANAGER", " "));

        assertThrows(BusinessException.class,
                () -> fixture.service.resolve(9L, 100L, MerchantPermission.ORDER_MANAGE));
    }

    private static TenantEmployee employee(String role, String scopeType) {
        TenantEmployee employee = new TenantEmployee();
        employee.setId(3L);
        employee.setTenantId(9L);
        employee.setPlatformUserId(100L);
        employee.setEmployeeRole(role);
        employee.setStoreScopeType(scopeType);
        employee.setStatus(1);
        return employee;
    }

    private static TenantEmployeeStore assignment(Long storeId) {
        TenantEmployeeStore assignment = new TenantEmployeeStore();
        assignment.setTenantId(9L);
        assignment.setEmployeeId(3L);
        assignment.setStoreId(storeId);
        return assignment;
    }

    private static Store store(Long id, Long tenantId, Integer deleted) {
        Store store = new Store();
        store.setId(id);
        store.setTenantId(tenantId);
        store.setDeleted(deleted);
        return store;
    }

    private static class Fixture {
        private final V1MerchantSupportService supportService = mock(V1MerchantSupportService.class);
        private final TenantEmployeeStoreMapper employeeStoreMapper = mock(TenantEmployeeStoreMapper.class);
        private final StoreMapper storeMapper = mock(StoreMapper.class);
        private final MerchantStoreScopeService service =
                new MerchantStoreScopeService(supportService, employeeStoreMapper, storeMapper);
    }
}
