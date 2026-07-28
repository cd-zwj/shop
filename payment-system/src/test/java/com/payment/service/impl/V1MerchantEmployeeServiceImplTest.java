package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.dto.V1MerchantEmployeeCreateDTO;
import com.payment.dto.V1MerchantEmployeeStoreScopeUpdateDTO;
import com.payment.entity.PlatformUser;
import com.payment.entity.Store;
import com.payment.entity.TenantEmployee;
import com.payment.entity.TenantEmployeeStore;
import com.payment.mapper.PlatformUserMapper;
import com.payment.mapper.StoreMapper;
import com.payment.mapper.TenantEmployeeMapper;
import com.payment.mapper.TenantEmployeeStoreMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class V1MerchantEmployeeServiceImplTest {

    private TenantEmployeeMapper tenantEmployeeMapper;
    private PlatformUserMapper platformUserMapper;
    private TenantEmployeeStoreMapper tenantEmployeeStoreMapper;
    private StoreMapper storeMapper;
    private V1MerchantSupportService supportService;
    private V1MerchantEmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        tenantEmployeeMapper = mock(TenantEmployeeMapper.class);
        platformUserMapper = mock(PlatformUserMapper.class);
        tenantEmployeeStoreMapper = mock(TenantEmployeeStoreMapper.class);
        storeMapper = mock(StoreMapper.class);
        supportService = mock(V1MerchantSupportService.class);
        service = new V1MerchantEmployeeServiceImpl(
                tenantEmployeeMapper, platformUserMapper, tenantEmployeeStoreMapper, storeMapper, supportService);
    }

    @Test
    void addEmployeeShouldCreateActiveEmployeeWithNormalizedRole() {
        TenantEmployee operator = employee(1L, 1L, 10L, "OWNER", 1);
        PlatformUser user = new PlatformUser();
        user.setId(20L);
        user.setUsername("operator");
        user.setStatus(1);
        user.setDeleted(0);
        when(supportService.requirePermission(1L, 10L, MerchantPermission.EMPLOYEE_MANAGE)).thenReturn(operator);
        when(platformUserMapper.selectById(20L)).thenReturn(user);
        when(tenantEmployeeMapper.selectOne(any())).thenReturn(null);

        V1MerchantEmployeeCreateDTO dto = new V1MerchantEmployeeCreateDTO();
        dto.setPlatformUserId(20L);
        dto.setEmployeeRole("finance");
        service.addEmployee(1L, 10L, dto);

        ArgumentCaptor<TenantEmployee> captor = ArgumentCaptor.forClass(TenantEmployee.class);
        verify(tenantEmployeeMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getTenantId());
        assertEquals(20L, captor.getValue().getPlatformUserId());
        assertEquals("FINANCE", captor.getValue().getEmployeeRole());
        assertEquals(1, captor.getValue().getStatus());
        assertEquals("ASSIGNED", captor.getValue().getStoreScopeType());
    }

    @Test
    void addEmployeeShouldPersistAssignedStores() {
        TenantEmployee operator = employee(1L, 1L, 10L, "OWNER", 1);
        PlatformUser user = new PlatformUser();
        user.setId(20L);
        user.setStatus(1);
        user.setDeleted(0);
        Store store = new Store();
        store.setId(7L);
        store.setTenantId(1L);
        store.setDeleted(0);
        when(supportService.requirePermission(1L, 10L, MerchantPermission.EMPLOYEE_MANAGE)).thenReturn(operator);
        when(platformUserMapper.selectById(20L)).thenReturn(user);
        when(tenantEmployeeMapper.selectOne(any())).thenReturn(null);
        when(storeMapper.selectBatchIds(any())).thenReturn(java.util.List.of(store));
        doAnswer(invocation -> {
            TenantEmployee inserted = invocation.getArgument(0);
            inserted.setId(2L);
            return 1;
        }).when(tenantEmployeeMapper).insert(any(TenantEmployee.class));
        V1MerchantEmployeeCreateDTO dto = new V1MerchantEmployeeCreateDTO();
        dto.setPlatformUserId(20L);
        dto.setEmployeeRole("MANAGER");
        dto.setStoreIds(java.util.List.of(7L));

        service.addEmployee(1L, 10L, dto);

        ArgumentCaptor<TenantEmployeeStore> assignment = ArgumentCaptor.forClass(TenantEmployeeStore.class);
        verify(tenantEmployeeStoreMapper).insert(assignment.capture());
        assertEquals(1L, assignment.getValue().getTenantId());
        assertEquals(2L, assignment.getValue().getEmployeeId());
        assertEquals(7L, assignment.getValue().getStoreId());
    }

    @Test
    void updateStoreScopeShouldRejectCrossTenantStoreBeforeReplacingAssignments() {
        TenantEmployee operator = employee(1L, 1L, 10L, "OWNER", 1);
        TenantEmployee target = employee(2L, 1L, 20L, "MANAGER", 1);
        Store otherTenantStore = new Store();
        otherTenantStore.setId(8L);
        otherTenantStore.setTenantId(2L);
        otherTenantStore.setDeleted(0);
        when(supportService.requirePermission(1L, 10L, MerchantPermission.EMPLOYEE_MANAGE)).thenReturn(operator);
        when(tenantEmployeeMapper.selectOne(any())).thenReturn(target);
        when(storeMapper.selectBatchIds(any())).thenReturn(java.util.List.of(otherTenantStore));
        V1MerchantEmployeeStoreScopeUpdateDTO dto = new V1MerchantEmployeeStoreScopeUpdateDTO();
        dto.setStoreScopeType("ASSIGNED");
        dto.setStoreIds(java.util.List.of(8L));

        assertThrows(BusinessException.class, () -> service.updateStoreScope(1L, 10L, 2L, dto));

        verify(tenantEmployeeStoreMapper, org.mockito.Mockito.never()).delete(any());
    }

    @Test
    void adminShouldNotBeAbleToGrantAllStores() {
        TenantEmployee operator = employee(1L, 1L, 10L, "ADMIN", 1);
        TenantEmployee target = employee(2L, 1L, 20L, "MANAGER", 1);
        when(supportService.requirePermission(1L, 10L, MerchantPermission.EMPLOYEE_MANAGE)).thenReturn(operator);
        when(tenantEmployeeMapper.selectOne(any())).thenReturn(target);
        V1MerchantEmployeeStoreScopeUpdateDTO dto = new V1MerchantEmployeeStoreScopeUpdateDTO();
        dto.setStoreScopeType("ALL");

        assertThrows(BusinessException.class, () -> service.updateStoreScope(1L, 10L, 2L, dto));
    }

    @Test
    void updateRoleShouldRejectSelfDemotionFromManagementRole() {
        TenantEmployee operator = employee(1L, 1L, 10L, "OWNER", 1);
        when(supportService.requirePermission(1L, 10L, MerchantPermission.EMPLOYEE_MANAGE)).thenReturn(operator);
        when(tenantEmployeeMapper.selectOne(any())).thenReturn(operator);

        assertThrows(BusinessException.class, () -> service.updateRole(1L, 10L, 1L, "OPERATOR"));
    }

    @Test
    void updateStatusShouldRejectDisablingLastOwner() {
        TenantEmployee operator = employee(1L, 1L, 10L, "OWNER", 1);
        when(supportService.requirePermission(1L, 10L, MerchantPermission.EMPLOYEE_MANAGE)).thenReturn(operator);
        when(tenantEmployeeMapper.selectOne(any())).thenReturn(operator);
        when(tenantEmployeeMapper.selectActiveOwnersForUpdate(1L)).thenReturn(java.util.List.of(operator));

        assertThrows(BusinessException.class, () -> service.updateStatus(1L, 10L, 1L, 0));
    }

    @Test
    void updateStatusShouldAllowDisablingOwnerWhenAnotherOwnerExists() {
        TenantEmployee operator = employee(1L, 2L, 10L, "OWNER", 1);
        TenantEmployee target = employee(2L, 1L, 20L, "OWNER", 1);
        when(supportService.requirePermission(1L, 10L, MerchantPermission.EMPLOYEE_MANAGE)).thenReturn(operator);
        when(tenantEmployeeMapper.selectOne(any())).thenReturn(target);
        when(tenantEmployeeMapper.selectActiveOwnersForUpdate(1L))
                .thenReturn(java.util.List.of(target, employee(3L, 1L, 30L, "OWNER", 1)));
        PlatformUser user = new PlatformUser();
        user.setId(20L);
        when(platformUserMapper.selectById(20L)).thenReturn(user);

        service.updateStatus(1L, 10L, 1L, 0);

        assertEquals(0, target.getStatus());
        verify(tenantEmployeeMapper).updateById(target);
        verify(tenantEmployeeMapper).selectActiveOwnersForUpdate(1L);
    }

    private TenantEmployee employee(Long id, Long tenantId, Long platformUserId, String role, Integer status) {
        TenantEmployee employee = new TenantEmployee();
        employee.setId(id);
        employee.setTenantId(tenantId);
        employee.setPlatformUserId(platformUserId);
        employee.setEmployeeRole(role);
        employee.setStatus(status);
        return employee;
    }
}
