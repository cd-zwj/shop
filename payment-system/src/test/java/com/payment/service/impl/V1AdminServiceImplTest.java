package com.payment.service.impl;

import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.config.AuthStpKit;
import com.payment.config.RbacPrincipalType;
import com.payment.dto.AdminDashboardOverviewVO;
import com.payment.dto.AdminPlatformUserVO;
import com.payment.dto.V1AdminSessionVO;
import com.payment.entity.PaymentBill;
import com.payment.entity.PlatformUser;
import com.payment.entity.RechargeOrderV1;
import com.payment.entity.SalesOrder;
import com.payment.mapper.MemberPointsAccountMapper;
import com.payment.mapper.MerchantWalletAccountMapper;
import com.payment.mapper.PaymentBillMapper;
import com.payment.mapper.PermissionMapper;
import com.payment.mapper.PlatformUserMapper;
import com.payment.mapper.RechargeOrderV1Mapper;
import com.payment.mapper.RoleMapper;
import com.payment.mapper.SalesOrderMapper;
import com.payment.mapper.TenantEmployeeMapper;
import com.payment.mapper.TenantMemberMapper;
import com.payment.mapper.UnifiedWalletAccountMapper;
import com.payment.service.AppOrderService;
import com.payment.service.MerchantService;
import com.payment.service.UserPermissionService;
import com.payment.service.UserService;
import com.payment.service.WithdrawalService;
import com.payment.util.AuthLoginIdHelper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class V1AdminServiceImplTest {

    @Test
    void adminSessionUsesPlatformUserIdAsRbacPrincipal() {
        UserService userService = mock(UserService.class);
        MerchantService merchantService = mock(MerchantService.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        UserPermissionService userPermissionService = mock(UserPermissionService.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        PlatformUserMapper platformUserMapper = mock(PlatformUserMapper.class);
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        TenantEmployeeMapper tenantEmployeeMapper = mock(TenantEmployeeMapper.class);
        UnifiedWalletAccountMapper unifiedWalletAccountMapper = mock(UnifiedWalletAccountMapper.class);
        MerchantWalletAccountMapper merchantWalletAccountMapper = mock(MerchantWalletAccountMapper.class);
        MemberPointsAccountMapper memberPointsAccountMapper = mock(MemberPointsAccountMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        PaymentBillMapper paymentBillMapper = mock(PaymentBillMapper.class);
        RechargeOrderV1Mapper rechargeOrderV1Mapper = mock(RechargeOrderV1Mapper.class);
        AppOrderService appOrderService = mock(AppOrderService.class);

        V1AdminServiceImpl service = new V1AdminServiceImpl(
                userService,
                merchantService,
                withdrawalService,
                userPermissionService,
                permissionMapper,
                roleMapper,
                platformUserMapper,
                tenantMemberMapper,
                tenantEmployeeMapper,
                unifiedWalletAccountMapper,
                merchantWalletAccountMapper,
                memberPointsAccountMapper,
                salesOrderMapper,
                paymentBillMapper,
                rechargeOrderV1Mapper,
                appOrderService
        );

        PlatformUser admin = new PlatformUser();
        admin.setId(99L);
        admin.setUsername("admin");
        admin.setStatus(1);
        admin.setDeleted(0);

        when(platformUserMapper.selectById(99L)).thenReturn(admin);
        when(roleMapper.selectRoleCodesByPrincipal(eq(99L), eq(RbacPrincipalType.ADMIN))).thenReturn(List.of("admin"));
        when(permissionMapper.selectPermissionCodesByPrincipal(eq(99L), eq(RbacPrincipalType.ADMIN))).thenReturn(List.of("admin:dashboard"));

        SaTokenContextMockUtil.setMockContext(() -> {
            AuthStpKit.ADMIN.login(AuthLoginIdHelper.admin(99L));
            V1AdminSessionVO session = service.getAdminSession();

            assertEquals(99L, session.getUserId());
            assertEquals("admin", session.getUsername());
            assertEquals(Integer.valueOf(2), session.getUserType());
            assertEquals(List.of("admin"), session.getRoles());
            assertEquals(List.of("admin:dashboard"), session.getPermissions());
            AuthStpKit.ADMIN.logout();
        });
    }
    @Test
    void dashboardOverviewShouldAggregateV1TradeData() {
        UserService userService = mock(UserService.class);
        MerchantService merchantService = mock(MerchantService.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        UserPermissionService userPermissionService = mock(UserPermissionService.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        PlatformUserMapper platformUserMapper = mock(PlatformUserMapper.class);
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        TenantEmployeeMapper tenantEmployeeMapper = mock(TenantEmployeeMapper.class);
        UnifiedWalletAccountMapper unifiedWalletAccountMapper = mock(UnifiedWalletAccountMapper.class);
        MerchantWalletAccountMapper merchantWalletAccountMapper = mock(MerchantWalletAccountMapper.class);
        MemberPointsAccountMapper memberPointsAccountMapper = mock(MemberPointsAccountMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        PaymentBillMapper paymentBillMapper = mock(PaymentBillMapper.class);
        RechargeOrderV1Mapper rechargeOrderV1Mapper = mock(RechargeOrderV1Mapper.class);
        AppOrderService appOrderService = mock(AppOrderService.class);

        V1AdminServiceImpl service = new V1AdminServiceImpl(
                userService,
                merchantService,
                withdrawalService,
                userPermissionService,
                permissionMapper,
                roleMapper,
                platformUserMapper,
                tenantMemberMapper,
                tenantEmployeeMapper,
                unifiedWalletAccountMapper,
                merchantWalletAccountMapper,
                memberPointsAccountMapper,
                salesOrderMapper,
                paymentBillMapper,
                rechargeOrderV1Mapper,
                appOrderService
        );

        when(platformUserMapper.selectCount(any())).thenReturn(10L);
        when(merchantService.count(any())).thenReturn(4L, 3L);
        // Mock SQL 聚合结果（selectMaps 返回单行 Map）
        when(salesOrderMapper.selectMaps(any())).thenReturn(List.of(
                java.util.Map.of("totalOrders", 2L, "totalOrderAmount", new BigDecimal("20.00"), "paidOrders", 1L)
        ));
        when(paymentBillMapper.selectMaps(any())).thenReturn(List.of(
                java.util.Map.of("totalBills", 2L, "totalPayAmount", new BigDecimal("15.00"))
        ));
        when(rechargeOrderV1Mapper.selectMaps(any())).thenReturn(List.of(
                java.util.Map.of("totalRecharge", 2L, "totalRechargeAmount", new BigDecimal("50.00"))
        ));
        Page<com.payment.dto.WithdrawalVO> withdrawalPage = new Page<>(1, 1, 2);
        when(withdrawalService.listWithdrawalsForAdmin(1, 1, null, 0, null, null)).thenReturn(withdrawalPage);

        AdminDashboardOverviewVO overview = service.getDashboardOverview();

        assertEquals(10L, overview.getTotalPlatformUsers());
        assertEquals(4L, overview.getTotalMerchants());
        assertEquals(3L, overview.getActiveMerchants());
        assertEquals(2L, overview.getTotalOrders());
        assertEquals(1L, overview.getPaidOrders());
        assertEquals(new BigDecimal("20.00"), overview.getTotalOrderAmount());
        assertEquals(2L, overview.getTotalPaymentBills());
        assertEquals(new BigDecimal("15.00"), overview.getTotalPaymentAmount());
        assertEquals(2L, overview.getTotalRechargeOrders());
        assertEquals(new BigDecimal("50.00"), overview.getTotalRechargeAmount());
        assertEquals(2L, overview.getPendingWithdrawals());
    }

    @Test
    void listPlatformUsersShouldFillCountsAndWalletBalance() {
        UserService userService = mock(UserService.class);
        MerchantService merchantService = mock(MerchantService.class);
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        UserPermissionService userPermissionService = mock(UserPermissionService.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        PlatformUserMapper platformUserMapper = mock(PlatformUserMapper.class);
        TenantMemberMapper tenantMemberMapper = mock(TenantMemberMapper.class);
        TenantEmployeeMapper tenantEmployeeMapper = mock(TenantEmployeeMapper.class);
        UnifiedWalletAccountMapper unifiedWalletAccountMapper = mock(UnifiedWalletAccountMapper.class);
        MerchantWalletAccountMapper merchantWalletAccountMapper = mock(MerchantWalletAccountMapper.class);
        MemberPointsAccountMapper memberPointsAccountMapper = mock(MemberPointsAccountMapper.class);
        SalesOrderMapper salesOrderMapper = mock(SalesOrderMapper.class);
        PaymentBillMapper paymentBillMapper = mock(PaymentBillMapper.class);
        RechargeOrderV1Mapper rechargeOrderV1Mapper = mock(RechargeOrderV1Mapper.class);
        AppOrderService appOrderService = mock(AppOrderService.class);

        V1AdminServiceImpl service = new V1AdminServiceImpl(
                userService,
                merchantService,
                withdrawalService,
                userPermissionService,
                permissionMapper,
                roleMapper,
                platformUserMapper,
                tenantMemberMapper,
                tenantEmployeeMapper,
                unifiedWalletAccountMapper,
                merchantWalletAccountMapper,
                memberPointsAccountMapper,
                salesOrderMapper,
                paymentBillMapper,
                rechargeOrderV1Mapper,
                appOrderService
        );

        PlatformUser platformUser = new PlatformUser();
        platformUser.setId(100L);
        platformUser.setUserNo("PU001");
        platformUser.setUsername("alice");
        platformUser.setPhone("13800000000");
        platformUser.setEmail("alice@test.com");
        platformUser.setStatus(1);

        Page<PlatformUser> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(platformUser));
        when(platformUserMapper.selectPage(any(), any())).thenReturn(page);
        when(tenantMemberMapper.selectCount(any())).thenReturn(5L);
        when(tenantEmployeeMapper.selectCount(any())).thenReturn(2L);
        when(merchantWalletAccountMapper.selectCount(any())).thenReturn(0L);
        when(memberPointsAccountMapper.selectCount(any())).thenReturn(0L);

        com.payment.entity.UnifiedWalletAccount walletAccount = new com.payment.entity.UnifiedWalletAccount();
        walletAccount.setAvailableAmount(new BigDecimal("88.00"));
        when(unifiedWalletAccountMapper.selectOne(any())).thenReturn(walletAccount);

        Page<AdminPlatformUserVO> result = service.listPlatformUsers(1, 10, "ali", 1);

        assertEquals(1, result.getRecords().size());
        AdminPlatformUserVO userVO = result.getRecords().get(0);
        assertEquals("alice", userVO.getUsername());
        assertEquals(5L, userVO.getMemberTenantCount());
        assertEquals(2L, userVO.getEmployeeTenantCount());
        assertEquals(new BigDecimal("88.00"), userVO.getUnifiedWalletBalance());
    }

    private SalesOrder buildSalesOrder(String status, String amount) {
        SalesOrder order = new SalesOrder();
        order.setOrderStatus(status);
        order.setPayStatus("PAID".equals(status) ? "SUCCESS" : "WAIT_PAY");
        order.setTotalAmount(new BigDecimal(amount));
        return order;
    }

    private PaymentBill buildPaymentBill(String amount) {
        PaymentBill bill = new PaymentBill();
        bill.setPayAmount(new BigDecimal(amount));
        return bill;
    }

    private RechargeOrderV1 buildRechargeOrder(String amount) {
        RechargeOrderV1 rechargeOrder = new RechargeOrderV1();
        rechargeOrder.setRechargeAmount(new BigDecimal(amount));
        return rechargeOrder;
    }
}
