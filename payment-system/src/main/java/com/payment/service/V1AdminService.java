package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.AdminDashboardOverviewVO;
import com.payment.dto.AdminOrderListVO;
import com.payment.dto.AdminPaymentBillVO;
import com.payment.dto.AdminPlatformUserVO;
import com.payment.dto.AdminRechargeOrderVO;
import com.payment.dto.AdminTradeOverviewVO;
import com.payment.dto.MerchantDTO;
import com.payment.dto.MerchantDetailVO;
import com.payment.dto.MerchantListVO;
import com.payment.dto.SalesOrderDetailVO;
import com.payment.dto.UserPermissionDTO;
import com.payment.dto.UserPermissionVO;
import com.payment.dto.V1AdminSessionVO;
import com.payment.dto.WithdrawalVO;
import com.payment.entity.Permission;
import com.payment.entity.Tenant;

import java.util.List;
import java.util.Map;

public interface V1AdminService {

    String login(String username, String password);

    V1AdminSessionVO getAdminSession();

    Map<String, Object> getAdminInfo();

    AdminDashboardOverviewVO getDashboardOverview();

    AdminTradeOverviewVO getTradeOverview();

    Page<AdminPlatformUserVO> listPlatformUsers(Integer current, Integer size, String keyword, Integer status);

    AdminPlatformUserVO getPlatformUserDetail(Long userId);

    void enablePlatformUser(Long userId);

    void disablePlatformUser(Long userId);

    Page<MerchantListVO> listMerchants(Integer current, Integer size, String name, Integer status);

    MerchantDetailVO getMerchantDetail(Long tenantId);

    Tenant createMerchant(MerchantDTO dto);

    void updateMerchant(Long tenantId, MerchantDTO dto);

    void enableMerchant(Long tenantId);

    void disableMerchant(Long tenantId);

    Page<WithdrawalVO> listWithdrawals(Integer current, Integer size, String merchantName, Integer status, String startDate, String endDate);

    void approveWithdrawal(Long withdrawalId);

    void rejectWithdrawal(Long withdrawalId, String reason);

    Map<String, List<Permission>> listPermissions();

    UserPermissionVO getUserPermissions(Long userId);

    void setUserPermissions(Long userId, UserPermissionDTO dto);

    void removeUserPermission(Long userId, Long permissionId);

    Page<AdminOrderListVO> listOrders(Integer current, Integer size, String orderNo, String orderStatus, String payStatus, Long tenantId);

    SalesOrderDetailVO getOrderDetail(String orderNo);

    Page<AdminPaymentBillVO> listPaymentBills(Integer current, Integer size, String bizType, String payStatus, String channelCode);

    Page<AdminRechargeOrderVO> listRechargeOrders(Integer current, Integer size, String walletType, String bizStatus, Long tenantId);
}
