package com.payment.rag.service.scenario;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.dto.AdminDashboardOverviewVO;
import com.payment.dto.SalesOverviewDTO;
import com.payment.dto.WalletAccountVO;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MerchantBalance;
import com.payment.rag.service.AuthContextService;
import com.payment.service.AppOrderService;
import com.payment.service.CouponService;
import com.payment.service.MemberPointsAccountService;
import com.payment.service.MerchantWalletService;
import com.payment.service.RefundApplicationService;
import com.payment.service.SalesStatisticsService;
import com.payment.service.UnifiedWalletService;
import com.payment.service.V1AdminService;
import com.payment.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioBusinessTools {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AuthContextService authContextService;
    private final UnifiedWalletService unifiedWalletService;
    private final MemberPointsAccountService memberPointsAccountService;
    private final CouponService couponService;
    private final AppOrderService appOrderService;
    private final SalesStatisticsService salesStatisticsService;
    private final RefundApplicationService refundApplicationService;
    private final MerchantWalletService merchantWalletService;
    private final WithdrawalService withdrawalService;
    private final V1AdminService v1AdminService;

    public String currentActorContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("userId", authContextService.getCurrentUserId());
        context.put("role", authContextService.getCurrentRole());
        context.put("tenantId", authContextService.getCurrentTenantId());
        context.put("permissions", authContextService.getCurrentPermissions());
        return json(context);
    }

    public String userDataContext() {
        requireRole("user");
        List<String> modules = permittedModules(Map.of(
                "ai:user:wallet", List.of("wallet", "points", "coupons"),
                "ai:user:orders", List.of("orders", "refunds", "notifications")
        ));
        Long userId = authContextService.getCurrentPlatformUserId();
        Long tenantId = authContextService.getCurrentTenantId();

        Map<String, Object> data = base("user_data_context");
        data.put("scope", "current_user_only");
        data.put("availableModules", modules);

        if (modules.contains("wallet")) {
            fillUserWallet(data, userId);
        }
        if (modules.contains("points")) {
            fillUserPoints(data, userId, tenantId);
        }
        if (modules.contains("coupons")) {
            fillUserCoupons(data, userId, tenantId);
        }
        if (modules.contains("orders")) {
            fillUserOrders(data, userId);
        }

        return json(data);
    }

    public String merchantDataContext() {
        requireRole("merchant");
        List<String> modules = permittedModules(Map.of(
                "ai:merchant:orders", List.of("orders", "refunds"),
                "ai:merchant:marketing", List.of("coupons", "activities", "members"),
                "ai:merchant:finance", List.of("finance")
        ));
        Long tenantId = authContextService.getCurrentTenantId();

        Map<String, Object> data = base("merchant_data_context");
        data.put("scope", "current_merchant_tenant_only");
        data.put("availableModules", modules);

        if (modules.stream().anyMatch(m -> List.of("orders", "refunds").contains(m))) {
            fillMerchantSales(data, tenantId);
            fillMerchantOrders(data, tenantId);
        }
        if (modules.contains("finance")) {
            fillMerchantFinance(data, tenantId);
        }

        return json(data);
    }

    public String adminDataContext() {
        requireRole("admin");
        List<String> modules = permittedModules(Map.of(
                "ai:admin:governance", List.of("merchants", "users", "transactions", "payments", "recharges", "withdrawals", "permissions"),
                "ai:admin:risk", List.of("risk")
        ));

        Map<String, Object> data = base("admin_data_context");
        data.put("scope", "platform_governance");
        data.put("availableModules", modules);

        if (modules.stream().anyMatch(m -> List.of("merchants", "users", "transactions", "payments").contains(m))) {
            fillAdminDashboard(data);
        }

        return json(data);
    }

    // ==================== 用户数据填充 ====================

    private void fillUserWallet(Map<String, Object> data, Long userId) {
        try {
            WalletAccountVO wallet = unifiedWalletService.getWallet(userId);
            if (wallet != null) {
                Map<String, Object> walletSummary = new LinkedHashMap<>();
                walletSummary.put("availableAmount", wallet.getAvailableAmount());
                walletSummary.put("frozenAmount", wallet.getFrozenAmount());
                walletSummary.put("totalRecharge", wallet.getTotalRecharge());
                walletSummary.put("totalConsume", wallet.getTotalConsume());
                data.put("wallet", walletSummary);
            }
        } catch (Exception e) {
            log.warn("AI 工具读取用户钱包失败: userId={}, error={}", userId, e.getMessage());
            data.put("walletError", "钱包数据暂时无法获取");
        }
    }

    private void fillUserPoints(Map<String, Object> data, Long userId, Long tenantId) {
        try {
            MemberPointsAccount account = memberPointsAccountService.getAccount(tenantId, userId);
            if (account != null) {
                Map<String, Object> pointsSummary = new LinkedHashMap<>();
                pointsSummary.put("currentPoints", account.getPoints());
                pointsSummary.put("totalEarned", account.getTotalEarned());
                pointsSummary.put("totalUsed", account.getTotalUsed());
                data.put("points", pointsSummary);
            }
        } catch (Exception e) {
            log.warn("AI 工具读取用户积分失败: userId={}, tenantId={}, error={}", userId, tenantId, e.getMessage());
            data.put("pointsError", "积分数据暂时无法获取");
        }
    }

    private void fillUserCoupons(Map<String, Object> data, Long userId, Long tenantId) {
        try {
            var unusedCoupons = couponService.listUserCoupons(tenantId, userId, "UNUSED");
            var usedCoupons = couponService.listUserCoupons(tenantId, userId, "USED");
            var expiredCoupons = couponService.listUserCoupons(tenantId, userId, "EXPIRED");
            Map<String, Object> couponSummary = new LinkedHashMap<>();
            couponSummary.put("unusedCount", unusedCoupons != null ? unusedCoupons.size() : 0);
            couponSummary.put("usedCount", usedCoupons != null ? usedCoupons.size() : 0);
            couponSummary.put("expiredCount", expiredCoupons != null ? expiredCoupons.size() : 0);
            data.put("coupons", couponSummary);
        } catch (Exception e) {
            log.warn("AI 工具读取用户优惠券失败: userId={}, tenantId={}, error={}", userId, tenantId, e.getMessage());
            data.put("couponsError", "优惠券数据暂时无法获取");
        }
    }

    private void fillUserOrders(Map<String, Object> data, Long userId) {
        try {
            var orders = appOrderService.listOrders(userId, 1, 1);
            Map<String, Object> orderSummary = new LinkedHashMap<>();
            orderSummary.put("totalOrders", orders.getTotal());
            if (orders.getRecords() != null && !orders.getRecords().isEmpty()) {
                var latest = orders.getRecords().get(0);
                Map<String, Object> latestOrder = new LinkedHashMap<>();
                latestOrder.put("orderNo", latest.getOrderNo());
                latestOrder.put("orderStatus", latest.getOrderStatus());
                latestOrder.put("totalAmount", latest.getTotalAmount());
                latestOrder.put("createTime", latest.getCreateTime() != null ? latest.getCreateTime().toString() : null);
                orderSummary.put("latestOrder", latestOrder);
            }
            data.put("orders", orderSummary);
        } catch (Exception e) {
            log.warn("AI 工具读取用户订单失败: userId={}, error={}", userId, e.getMessage());
            data.put("ordersError", "订单数据暂时无法获取");
        }
    }

    // ==================== 商家数据填充 ====================

    private void fillMerchantSales(Map<String, Object> data, Long tenantId) {
        try {
            SalesOverviewDTO overview = salesStatisticsService.getSalesOverview(tenantId);
            if (overview != null) {
                Map<String, Object> salesSummary = new LinkedHashMap<>();
                salesSummary.put("todaySales", overview.getTodaySales());
                salesSummary.put("todayOrderCount", overview.getTodayOrderCount());
                salesSummary.put("monthSales", overview.getMonthSales());
                salesSummary.put("monthOrderCount", overview.getMonthOrderCount());
                salesSummary.put("totalSales", overview.getTotalSales());
                salesSummary.put("totalOrderCount", overview.getTotalOrderCount());
                data.put("salesOverview", salesSummary);
            }
        } catch (Exception e) {
            log.warn("AI 工具读取商家销售数据失败: tenantId={}, error={}", tenantId, e.getMessage());
            data.put("salesError", "销售数据暂时无法获取");
        }
    }

    private void fillMerchantOrders(Map<String, Object> data, Long tenantId) {
        try {
            var paidOrders = appOrderService.listMerchantOrders(tenantId, 1, 1, "PAID", null, null);
            var pendingRefunds = refundApplicationService.listTenantRefunds(tenantId, "PENDING", 1, 1);
            Map<String, Object> orderOps = new LinkedHashMap<>();
            orderOps.put("pendingShipmentCount", paidOrders != null ? paidOrders.getTotal() : 0);
            orderOps.put("pendingRefundReviewCount", pendingRefunds != null ? pendingRefunds.getTotal() : 0);
            data.put("orderOperations", orderOps);
        } catch (Exception e) {
            log.warn("AI 工具读取商家订单运营数据失败: tenantId={}, error={}", tenantId, e.getMessage());
            data.put("orderOpsError", "订单运营数据暂时无法获取");
        }
    }

    private void fillMerchantFinance(Map<String, Object> data, Long tenantId) {
        try {
            MerchantBalance balance = withdrawalService.getMerchantBalance(tenantId);
            if (balance != null) {
                Map<String, Object> financeSummary = new LinkedHashMap<>();
                financeSummary.put("balance", balance.getBalance());
                financeSummary.put("frozenBalance", balance.getFrozenBalance());
                financeSummary.put("totalIncome", balance.getTotalIncome());
                financeSummary.put("totalWithdrawal", balance.getTotalWithdrawal());
                data.put("finance", financeSummary);
            }
        } catch (Exception e) {
            log.warn("AI 工具读取商家财务数据失败: tenantId={}, error={}", tenantId, e.getMessage());
            data.put("financeError", "财务数据暂时无法获取");
        }
    }

    // ==================== 管理员数据填充 ====================

    private void fillAdminDashboard(Map<String, Object> data) {
        try {
            AdminDashboardOverviewVO overview = v1AdminService.getDashboardOverview();
            if (overview != null) {
                Map<String, Object> dashboardSummary = new LinkedHashMap<>();
                dashboardSummary.put("totalPlatformUsers", overview.getTotalPlatformUsers());
                dashboardSummary.put("totalMerchants", overview.getTotalMerchants());
                dashboardSummary.put("activeMerchants", overview.getActiveMerchants());
                dashboardSummary.put("totalOrders", overview.getTotalOrders());
                dashboardSummary.put("paidOrders", overview.getPaidOrders());
                dashboardSummary.put("totalOrderAmount", overview.getTotalOrderAmount());
                dashboardSummary.put("totalPaymentBills", overview.getTotalPaymentBills());
                dashboardSummary.put("totalPaymentAmount", overview.getTotalPaymentAmount());
                dashboardSummary.put("pendingWithdrawals", overview.getPendingWithdrawals());
                data.put("dashboard", dashboardSummary);
            }
        } catch (Exception e) {
            log.warn("AI 工具读取管理员看板失败: error={}", e.getMessage());
            data.put("dashboardError", "平台数据暂时无法获取");
        }
    }

    // ==================== 基础方法 ====================

    private Map<String, Object> base(String toolName) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tool", toolName);
        data.put("userId", authContextService.getCurrentUserId());
        data.put("role", authContextService.getCurrentRole());
        data.put("tenantId", authContextService.getCurrentTenantId());
        return data;
    }

    private void requireRole(String requiredRole) {
        String role = authContextService.getCurrentRole();
        if (!requiredRole.equals(role)) {
            throw new IllegalArgumentException("当前角色无权调用该 AI 工具");
        }
    }

    private List<String> permittedModules(Map<String, List<String>> permissionModules) {
        List<String> permissions = authContextService.getCurrentPermissions();
        requireAnyPermission(permissionModules.keySet(), permissions);
        List<String> modules = new ArrayList<>();
        permissionModules.forEach((permission, names) -> {
            if (permissions.contains(permission)) {
                modules.addAll(names);
            }
        });
        return modules;
    }

    private void requireAnyPermission(Collection<String> requiredPermissions, List<String> permissions) {
        boolean allowed = permissions != null && requiredPermissions.stream().anyMatch(permissions::contains);
        if (!allowed) {
            throw new IllegalArgumentException("当前权限无权调用该 AI 工具");
        }
    }

    private String json(Map<String, Object> value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
