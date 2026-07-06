package com.payment.service.impl;

import com.payment.common.BusinessException;
import com.payment.constant.MerchantPermission;
import com.payment.entity.Tenant;
import com.payment.entity.TenantEmployee;
import com.payment.mapper.TenantEmployeeMapper;
import com.payment.mapper.TenantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class V1MerchantSupportServiceTest {

    private TenantEmployeeMapper tenantEmployeeMapper;
    private TenantMapper tenantMapper;
    private V1MerchantSupportService service;

    @BeforeEach
    void setUp() {
        tenantEmployeeMapper = mock(TenantEmployeeMapper.class);
        tenantMapper = mock(TenantMapper.class);
        service = new V1MerchantSupportService(tenantEmployeeMapper, tenantMapper);
        when(tenantMapper.selectById(1L)).thenReturn(activeTenant());
    }

    @Test
    void ownerShouldAccessEveryMerchantPermission() {
        when(tenantEmployeeMapper.selectOne(any())).thenReturn(employee("OWNER"));

        for (MerchantPermission permission : MerchantPermission.values()) {
            assertDoesNotThrow(() -> service.requirePermission(1L, 100L, permission));
        }
    }

    @Test
    void financeShouldAccessFinanceAndWithdrawalOnly() {
        when(tenantEmployeeMapper.selectOne(any())).thenReturn(employee("FINANCE"));

        assertDoesNotThrow(() -> service.requirePermission(1L, 100L, MerchantPermission.FINANCE_VIEW));
        assertDoesNotThrow(() -> service.requirePermission(1L, 100L, MerchantPermission.WITHDRAWAL_MANAGE));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.requirePermission(1L, 100L, MerchantPermission.PRODUCT_MANAGE));
        assertEquals("当前员工无权操作该商户模块", ex.getMessage());
    }

    @Test
    void cashierShouldAccessOrdersAndRefundsOnly() {
        when(tenantEmployeeMapper.selectOne(any())).thenReturn(employee(" cashier "));

        assertDoesNotThrow(() -> service.requirePermission(1L, 100L, MerchantPermission.ORDER_MANAGE));
        assertDoesNotThrow(() -> service.requirePermission(1L, 100L, MerchantPermission.REFUND_MANAGE));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.requirePermission(1L, 100L, MerchantPermission.FINANCE_VIEW));
        assertEquals("当前员工无权操作该商户模块", ex.getMessage());
    }

    @Test
    void unknownRoleShouldOnlyAccessDashboard() {
        when(tenantEmployeeMapper.selectOne(any())).thenReturn(employee("TEMP"));

        assertDoesNotThrow(() -> service.requirePermission(1L, 100L, MerchantPermission.DASHBOARD_VIEW));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.requirePermission(1L, 100L, MerchantPermission.ORDER_MANAGE));
        assertEquals("当前员工无权操作该商户模块", ex.getMessage());
    }

    private TenantEmployee employee(String role) {
        TenantEmployee employee = new TenantEmployee();
        employee.setTenantId(1L);
        employee.setPlatformUserId(100L);
        employee.setEmployeeRole(role);
        employee.setStatus(1);
        return employee;
    }

    private Tenant activeTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setStatus(1);
        tenant.setDeleted(0);
        return tenant;
    }
}
