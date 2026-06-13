package com.payment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.AdminDashboardOverviewVO;
import com.payment.dto.AdminTrendVO;
import com.payment.dto.AdminOrderListVO;
import com.payment.dto.AdminPaymentBillVO;
import com.payment.dto.AdminPlatformUserVO;
import com.payment.dto.AdminRechargeOrderVO;
import com.payment.dto.AdminTradeOverviewVO;
import com.payment.dto.LoginDTO;
import com.payment.dto.MerchantDTO;
import com.payment.dto.MerchantDetailVO;
import com.payment.dto.MerchantListVO;
import com.payment.dto.MerchantQueryDTO;
import com.payment.dto.SalesOrderDetailVO;
import com.payment.dto.UserPermissionDTO;
import com.payment.dto.UserPermissionVO;
import com.payment.dto.V1AdminSessionVO;
import com.payment.dto.WithdrawalApproveDTO;
import com.payment.dto.WithdrawalVO;
import com.payment.entity.MemberPointsAccount;
import com.payment.entity.MerchantWalletAccount;
import com.payment.entity.PaymentBill;
import com.payment.entity.Permission;
import com.payment.entity.PlatformUser;
import com.payment.entity.RechargeOrderV1;
import com.payment.entity.SalesOrder;
import com.payment.entity.Tenant;
import com.payment.entity.TenantEmployee;
import com.payment.entity.TenantMember;
import com.payment.entity.UnifiedWalletAccount;
import com.payment.entity.User;
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
import com.payment.service.V1AdminService;
import com.payment.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class V1AdminServiceImpl implements V1AdminService {

    private final UserService userService;
    private final MerchantService merchantService;
    private final WithdrawalService withdrawalService;
    private final UserPermissionService userPermissionService;
    private final PermissionMapper permissionMapper;
    private final RoleMapper roleMapper;
    private final PlatformUserMapper platformUserMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final TenantEmployeeMapper tenantEmployeeMapper;
    private final UnifiedWalletAccountMapper unifiedWalletAccountMapper;
    private final MerchantWalletAccountMapper merchantWalletAccountMapper;
    private final MemberPointsAccountMapper memberPointsAccountMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final PaymentBillMapper paymentBillMapper;
    private final RechargeOrderV1Mapper rechargeOrderV1Mapper;
    private final AppOrderService appOrderService;

    @Override
    public String login(String username, String password) {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername(username);
        loginDTO.setPassword(password);
        return userService.loginadmin(loginDTO);
    }

    @Override
    public V1AdminSessionVO getAdminSession() {
        Long loginId = StpUtil.getLoginIdAsLong();
        User user = (User) StpUtil.getSession().get("user");
        if (user == null) {
            user = userService.getById(loginId);
        }
        if (user == null || user.getStatus() == 0 || !Integer.valueOf(2).equals(user.getUserType())) {
            throw new BusinessException("管理员会话不存在或已失效");
        }

        V1AdminSessionVO vo = new V1AdminSessionVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setUserType(user.getUserType());
        vo.setRole("ADMIN");
        vo.setScope("V1_ADMIN");
        vo.setRoles(roleMapper.selectRoleCodesByUserId(user.getId()));
        vo.setPermissions(permissionMapper.selectPermissionCodesByUserId(user.getId()));
        return vo;
    }

    @Override
    public Map<String, Object> getAdminInfo() {
        V1AdminSessionVO session = getAdminSession();
        Map<String, Object> info = new HashMap<>();
        info.put("userId", session.getUserId());
        info.put("username", session.getUsername());
        info.put("nickname", session.getNickname());
        info.put("role", session.getRole());
        info.put("scope", session.getScope());
        info.put("roles", session.getRoles());
        info.put("permissions", session.getPermissions());
        return info;
    }

    @Override
    public AdminDashboardOverviewVO getDashboardOverview() {
        AdminDashboardOverviewVO vo = new AdminDashboardOverviewVO();

        // SQL 端聚合，避免全表加载到内存（修复 OOM 风险）
        Map<String, Object> orderStats = firstOrEmpty(salesOrderMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SalesOrder>()
                        .select("COUNT(*) AS totalOrders",
                                "COALESCE(SUM(total_amount), 0) AS totalOrderAmount",
                                "SUM(CASE WHEN pay_status = 'SUCCESS' THEN 1 ELSE 0 END) AS paidOrders")
                        .eq("deleted", 0)));

        Map<String, Object> billStats = firstOrEmpty(paymentBillMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PaymentBill>()
                        .select("COUNT(*) AS totalBills",
                                "COALESCE(SUM(pay_amount), 0) AS totalPayAmount")));

        Map<String, Object> rechargeStats = firstOrEmpty(rechargeOrderV1Mapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RechargeOrderV1>()
                        .select("COUNT(*) AS totalRecharge",
                                "COALESCE(SUM(recharge_amount), 0) AS totalRechargeAmount")
                        .eq("deleted", 0)));

        vo.setTotalPlatformUsers(platformUserMapper.selectCount(new LambdaQueryWrapper<PlatformUser>()
                .eq(PlatformUser::getDeleted, 0)));
        vo.setTotalMerchants(merchantService.count(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getDeleted, 0)));
        vo.setActiveMerchants(merchantService.count(new LambdaQueryWrapper<Tenant>()
                .eq(Tenant::getDeleted, 0)
                .eq(Tenant::getStatus, 1)));
        vo.setTotalOrders(toLong(orderStats.get("totalOrders")));
        vo.setPaidOrders(toLong(orderStats.get("paidOrders")));
        vo.setTotalOrderAmount(toBigDecimal(orderStats.get("totalOrderAmount")));
        vo.setTotalPaymentBills(toLong(billStats.get("totalBills")));
        vo.setTotalPaymentAmount(toBigDecimal(billStats.get("totalPayAmount")));
        vo.setTotalRechargeOrders(toLong(rechargeStats.get("totalRecharge")));
        vo.setTotalRechargeAmount(toBigDecimal(rechargeStats.get("totalRechargeAmount")));
        vo.setPendingWithdrawals(withdrawalService.listWithdrawalsForAdmin(1, 1, null, 0, null, null).getTotal());
        return vo;
    }

    @Override
    public AdminTrendVO getTrend(String startDate, String endDate, String granularity) {
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : end.minusDays(29);
        String gran = (granularity == null || granularity.isBlank()) ? "DAY" : granularity.toUpperCase();

        // 订单聚合
        String dateExpr = dateExpr(gran);
        List<Map<String, Object>> orderRows = salesOrderMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SalesOrder>()
                        .select(dateExpr + " AS period",
                                "COUNT(*) AS orderCount",
                                "COALESCE(SUM(total_amount), 0) AS orderAmount")
                        .eq("deleted", 0)
                        .ge("create_time", start.atStartOfDay())
                        .lt("create_time", end.plusDays(1).atStartOfDay())
                        .groupBy(dateExpr)
                        .orderByAsc("period"));

        // 用户增长聚合（platform_user 在 IGNORE_TABLES 中，无租户条件，查全平台注册）
        List<Map<String, Object>> userRows = platformUserMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PlatformUser>()
                        .select(dateExpr + " AS period", "COUNT(*) AS newUsers")
                        .eq("deleted", 0)
                        .ge("create_time", start.atStartOfDay())
                        .lt("create_time", end.plusDays(1).atStartOfDay())
                        .groupBy(dateExpr)
                        .orderByAsc("period"));

        // 用 Map<String, Object[]> 保留 BigDecimal 精度：[orderCount, orderAmount, newUsers]
        Map<String, Object[]> dataMap = new HashMap<>();
        for (Map<String, Object> row : orderRows) {
            String period = String.valueOf(row.get("period"));
            Object[] v = dataMap.computeIfAbsent(period, k -> new Object[]{0L, BigDecimal.ZERO, 0L});
            v[0] = row.get("orderCount") instanceof Number ? ((Number) row.get("orderCount")).longValue() : 0L;
            v[1] = row.get("orderAmount") instanceof Number ? new BigDecimal(row.get("orderAmount").toString()) : BigDecimal.ZERO;
        }
        for (Map<String, Object> row : userRows) {
            String period = String.valueOf(row.get("period"));
            Object[] v = dataMap.computeIfAbsent(period, k -> new Object[]{0L, BigDecimal.ZERO, 0L});
            v[2] = row.get("newUsers") instanceof Number ? ((Number) row.get("newUsers")).longValue() : 0L;
        }

        // 按粒度遍历并填充空位
        List<AdminTrendVO.TrendPoint> points = new ArrayList<>();
        for (LocalDate d = startDateForGranularity(start, gran);
             !d.isAfter(end);
             d = nextDate(d, gran)) {

            String key = dateToKey(d, gran);
            Object[] v = dataMap.getOrDefault(key, new Object[]{0L, BigDecimal.ZERO, 0L});
            AdminTrendVO.TrendPoint p = new AdminTrendVO.TrendPoint();
            p.setDate(key);
            p.setOrderCount(v[0] instanceof Number ? ((Number) v[0]).longValue() : 0L);
            p.setOrderAmount(v[1] instanceof BigDecimal ? (BigDecimal) v[1] : BigDecimal.ZERO);
            p.setNewUsers(v[2] instanceof Number ? ((Number) v[2]).longValue() : 0L);
            points.add(p);
        }

        AdminTrendVO vo = new AdminTrendVO();
        vo.setPoints(points);
        return vo;
    }

    /** 按粒度生成 SQL 日期表达式。 */
    private String dateExpr(String gran) {
        return switch (gran) {
            case "WEEK" -> "DATE_FORMAT(create_time, '%x-%v')";   // ISO 年-周
            case "MONTH" -> "DATE_FORMAT(create_time, '%Y-%m')";  // 年-月
            default -> "DATE(create_time)";                        // 默认日
        };
    }

    /** 按粒度返回遍历起点（WEEK 对齐到周一）。 */
    private LocalDate startDateForGranularity(LocalDate start, String gran) {
        if ("WEEK".equals(gran)) {
            return start.with(java.time.DayOfWeek.MONDAY);
        }
        return start;
    }

    /** 按粒度步进到下一个区间。 */
    private LocalDate nextDate(LocalDate current, String gran) {
        return switch (gran) {
            case "WEEK" -> current.plusWeeks(1);
            case "MONTH" -> current.plusMonths(1);
            default -> current.plusDays(1);
        };
    }

    /** 将 LocalDate 转为与 SQL 聚合键一致的字符串。 */
    private String dateToKey(LocalDate d, String gran) {
        return switch (gran) {
            case "WEEK" -> {
                java.time.temporal.WeekFields wf = java.time.temporal.WeekFields.ISO;
                int week = d.get(wf.weekOfWeekBasedYear());
                int year = d.get(wf.weekBasedYear());
                yield String.format("%04d-%02d", year, week);
            }
            case "MONTH" -> d.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            default -> d.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        };
    }

    private AdminTrendVO.TrendPoint emptyPoint(String date) {
        AdminTrendVO.TrendPoint p = new AdminTrendVO.TrendPoint();
        p.setDate(date);
        p.setOrderCount(0L);
        p.setOrderAmount(BigDecimal.ZERO);
        p.setNewUsers(0L);
        return p;
    }

    @Override
    public AdminTradeOverviewVO getTradeOverview() {
        // SQL 端聚合，避免全表加载到内存（修复 OOM 风险）
        Map<String, Object> orderStats = firstOrEmpty(salesOrderMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SalesOrder>()
                        .select("COUNT(*) AS totalOrders",
                                "COALESCE(SUM(total_amount), 0) AS totalOrderAmount",
                                "COALESCE(SUM(external_pay_amount), 0) AS totalExternalPayAmount",
                                "SUM(CASE WHEN pay_status = 'SUCCESS' THEN 1 ELSE 0 END) AS paidOrders",
                                "SUM(CASE WHEN pay_status IN ('WAIT_PAY','PAYING') THEN 1 ELSE 0 END) AS pendingOrders")
                        .eq("deleted", 0)));

        Map<String, Object> billStats = firstOrEmpty(paymentBillMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PaymentBill>()
                        .select("COUNT(*) AS totalBills",
                                "COALESCE(SUM(pay_amount), 0) AS totalPayAmount",
                                "SUM(CASE WHEN pay_status = 'SUCCESS' THEN 1 ELSE 0 END) AS paidBills")));

        Map<String, Object> rechargeStats = firstOrEmpty(rechargeOrderV1Mapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RechargeOrderV1>()
                        .select("COUNT(*) AS totalRecharge",
                                "COALESCE(SUM(recharge_amount), 0) AS totalRechargeAmount",
                                "SUM(CASE WHEN biz_status = 'SUCCESS' THEN 1 ELSE 0 END) AS successRecharge")
                        .eq("deleted", 0)));

        AdminTradeOverviewVO vo = new AdminTradeOverviewVO();
        vo.setTotalOrders(toLong(orderStats.get("totalOrders")));
        vo.setPaidOrders(toLong(orderStats.get("paidOrders")));
        vo.setPendingOrders(toLong(orderStats.get("pendingOrders")));
        vo.setTotalOrderAmount(toBigDecimal(orderStats.get("totalOrderAmount")));
        vo.setTotalExternalPayAmount(toBigDecimal(orderStats.get("totalExternalPayAmount")));
        vo.setTotalPaymentBills(toLong(billStats.get("totalBills")));
        vo.setPaidPaymentBills(toLong(billStats.get("paidBills")));
        vo.setTotalPaymentAmount(toBigDecimal(billStats.get("totalPayAmount")));
        vo.setTotalRechargeOrders(toLong(rechargeStats.get("totalRecharge")));
        vo.setSuccessRechargeOrders(toLong(rechargeStats.get("successRecharge")));
        vo.setTotalRechargeAmount(toBigDecimal(rechargeStats.get("totalRechargeAmount")));
        return vo;
    }

    @Override
    public Page<AdminPlatformUserVO> listPlatformUsers(Integer current, Integer size, String keyword, Integer status) {
        Page<PlatformUser> page = new Page<>(current, size);
        LambdaQueryWrapper<PlatformUser> wrapper = new LambdaQueryWrapper<PlatformUser>()
                .eq(PlatformUser::getDeleted, 0)
                .eq(status != null, PlatformUser::getStatus, status)
                .and(keyword != null && !keyword.isBlank(), q -> q.like(PlatformUser::getUsername, keyword)
                        .or()
                        .like(PlatformUser::getPhone, keyword)
                        .or()
                        .like(PlatformUser::getEmail, keyword))
                .orderByDesc(PlatformUser::getCreateTime);

        Page<PlatformUser> userPage = platformUserMapper.selectPage(page, wrapper);
        Page<AdminPlatformUserVO> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        result.setRecords(userPage.getRecords().stream().map(this::toPlatformUserVO).collect(Collectors.toList()));
        return result;
    }

    @Override
    public AdminPlatformUserVO getPlatformUserDetail(Long userId) {
        PlatformUser platformUser = platformUserMapper.selectById(userId);
        if (platformUser == null || platformUser.getDeleted() == 1) {
            throw new BusinessException("平台用户不存在");
        }
        return toPlatformUserVO(platformUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enablePlatformUser(Long userId) {
        updatePlatformUserStatus(userId, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disablePlatformUser(Long userId) {
        updatePlatformUserStatus(userId, 0);
    }

    @Override
    public Page<MerchantListVO> listMerchants(Integer current, Integer size, String name, Integer status) {
        MerchantQueryDTO queryDTO = new MerchantQueryDTO();
        queryDTO.setPageNum(current);
        queryDTO.setPageSize(size);
        queryDTO.setName(name);
        queryDTO.setStatus(status);

        Page<Tenant> tenantPage = merchantService.listMerchants(queryDTO);
        Page<MerchantListVO> result = new Page<>(tenantPage.getCurrent(), tenantPage.getSize(), tenantPage.getTotal());
        result.setRecords(tenantPage.getRecords().stream().map(tenant -> {
            MerchantListVO vo = new MerchantListVO();
            vo.setId(tenant.getId());
            vo.setTenantCode(tenant.getTenantCode());
            vo.setName(tenant.getName());
            vo.setContactName(tenant.getContact());
            vo.setContactPhone(tenant.getPhone());
            vo.setStatus(tenant.getStatus());
            vo.setCreateTime(tenant.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    @Override
    public MerchantDetailVO getMerchantDetail(Long tenantId) {
        return merchantService.getMerchantDetail(tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tenant createMerchant(MerchantDTO dto) {
        return merchantService.createMerchant(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMerchant(Long tenantId, MerchantDTO dto) {
        merchantService.updateMerchant(tenantId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableMerchant(Long tenantId) {
        merchantService.enableMerchant(tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableMerchant(Long tenantId) {
        merchantService.disableMerchant(tenantId);
    }

    @Override
    public Page<WithdrawalVO> listWithdrawals(Integer current, Integer size, String merchantName, Integer status, String startDate, String endDate) {
        return withdrawalService.listWithdrawalsForAdmin(current, size, merchantName, status, startDate, endDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveWithdrawal(Long withdrawalId, Long approverId) {
        withdrawalService.approveWithdrawal(approverId, buildWithdrawalApproveDTO(withdrawalId, true, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectWithdrawal(Long withdrawalId, Long approverId, String reason) {
        withdrawalService.approveWithdrawal(approverId, buildWithdrawalApproveDTO(withdrawalId, false, reason));
    }

    @Override
    public Map<String, List<Permission>> listPermissions() {
        return permissionMapper.selectList(null).stream().collect(Collectors.groupingBy(Permission::getModule));
    }

    @Override
    public UserPermissionVO getUserPermissions(Long userId) {
        return userPermissionService.getUserPermissions(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserPermissions(Long userId, UserPermissionDTO dto) {
        userPermissionService.setUserPermissions(userId, dto.getPermissionIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserPermission(Long userId, Long permissionId) {
        userPermissionService.revokePermission(userId, permissionId);
    }

    @Override
    public Page<AdminOrderListVO> listOrders(Integer current, Integer size, String orderNo, String orderStatus, String payStatus, Long tenantId) {
        Page<SalesOrder> page = new Page<>(current, size);
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getDeleted, 0)
                .eq(tenantId != null, SalesOrder::getTenantId, tenantId)
                .eq(orderStatus != null && !orderStatus.isBlank(), SalesOrder::getOrderStatus, orderStatus)
                .eq(payStatus != null && !payStatus.isBlank(), SalesOrder::getPayStatus, payStatus)
                .like(orderNo != null && !orderNo.isBlank(), SalesOrder::getOrderNo, orderNo)
                .orderByDesc(SalesOrder::getCreateTime);
        Page<SalesOrder> orderPage = salesOrderMapper.selectPage(page, wrapper);

        Page<AdminOrderListVO> result = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        result.setRecords(orderPage.getRecords().stream().map(order -> {
            AdminOrderListVO vo = new AdminOrderListVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setTenantId(order.getTenantId());
            vo.setPlatformUserId(order.getPlatformUserId());
            vo.setSubject(order.getSubject());
            vo.setOrderStatus(order.getOrderStatus());
            vo.setPayStatus(order.getPayStatus());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setExternalPayAmount(order.getExternalPayAmount());
            vo.setCreateTime(order.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    @Override
    public SalesOrderDetailVO getOrderDetail(String orderNo) {
        SalesOrder salesOrder = salesOrderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getOrderNo, orderNo)
                .eq(SalesOrder::getDeleted, 0));
        if (salesOrder == null) {
            throw new BusinessException("订单不存在");
        }
        return appOrderService.getOrderDetail(salesOrder.getPlatformUserId(), orderNo);
    }

    @Override
    public Page<AdminPaymentBillVO> listPaymentBills(Integer current, Integer size, String bizType, String payStatus, String channelCode) {
        Page<PaymentBill> page = new Page<>(current, size);
        LambdaQueryWrapper<PaymentBill> wrapper = new LambdaQueryWrapper<PaymentBill>()
                .eq(bizType != null && !bizType.isBlank(), PaymentBill::getBizType, bizType)
                .eq(payStatus != null && !payStatus.isBlank(), PaymentBill::getPayStatus, payStatus)
                .eq(channelCode != null && !channelCode.isBlank(), PaymentBill::getChannelCode, channelCode)
                .orderByDesc(PaymentBill::getCreateTime);
        Page<PaymentBill> billPage = paymentBillMapper.selectPage(page, wrapper);

        Page<AdminPaymentBillVO> result = new Page<>(billPage.getCurrent(), billPage.getSize(), billPage.getTotal());
        result.setRecords(billPage.getRecords().stream().map(bill -> {
            AdminPaymentBillVO vo = new AdminPaymentBillVO();
            vo.setId(bill.getId());
            vo.setBillNo(bill.getBillNo());
            vo.setBizType(bill.getBizType());
            vo.setBizNo(bill.getBizNo());
            vo.setTenantId(bill.getTenantId());
            vo.setPlatformUserId(bill.getPlatformUserId());
            vo.setChannelCode(bill.getChannelCode());
            vo.setPayStatus(bill.getPayStatus());
            vo.setPayAmount(bill.getPayAmount());
            vo.setCallbackStatus(bill.getCallbackStatus());
            vo.setThirdPartyBillNo(bill.getThirdPartyBillNo());
            vo.setCreateTime(bill.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    @Override
    public Page<AdminRechargeOrderVO> listRechargeOrders(Integer current, Integer size, String walletType, String bizStatus, Long tenantId) {
        Page<RechargeOrderV1> page = new Page<>(current, size);
        LambdaQueryWrapper<RechargeOrderV1> wrapper = new LambdaQueryWrapper<RechargeOrderV1>()
                .eq(RechargeOrderV1::getDeleted, 0)
                .eq(walletType != null && !walletType.isBlank(), RechargeOrderV1::getWalletType, walletType)
                .eq(bizStatus != null && !bizStatus.isBlank(), RechargeOrderV1::getBizStatus, bizStatus)
                .eq(tenantId != null, RechargeOrderV1::getTenantId, tenantId)
                .orderByDesc(RechargeOrderV1::getCreateTime);
        Page<RechargeOrderV1> rechargePage = rechargeOrderV1Mapper.selectPage(page, wrapper);

        Page<AdminRechargeOrderVO> result = new Page<>(rechargePage.getCurrent(), rechargePage.getSize(), rechargePage.getTotal());
        result.setRecords(rechargePage.getRecords().stream().map(recharge -> {
            AdminRechargeOrderVO vo = new AdminRechargeOrderVO();
            vo.setId(recharge.getId());
            vo.setRechargeNo(recharge.getRechargeNo());
            vo.setWalletType(recharge.getWalletType());
            vo.setTenantId(recharge.getTenantId());
            vo.setPlatformUserId(recharge.getPlatformUserId());
            vo.setRechargeAmount(recharge.getRechargeAmount());
            vo.setGiftAmount(recharge.getGiftAmount());
            vo.setGiftPoints(recharge.getGiftPoints());
            vo.setActualCreditAmount(recharge.getActualCreditAmount());
            vo.setBizStatus(recharge.getBizStatus());
            vo.setCreateTime(recharge.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
        return result;
    }

    private void updatePlatformUserStatus(Long userId, Integer status) {
        PlatformUser platformUser = platformUserMapper.selectById(userId);
        if (platformUser == null || platformUser.getDeleted() == 1) {
            throw new BusinessException("平台用户不存在");
        }
        platformUser.setStatus(status);
        platformUserMapper.updateById(platformUser);
    }

    private AdminPlatformUserVO toPlatformUserVO(PlatformUser user) {
        AdminPlatformUserVO vo = new AdminPlatformUserVO();
        vo.setId(user.getId());
        vo.setUserNo(user.getUserNo());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setMemberTenantCount(tenantMemberMapper.selectCount(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getPlatformUserId, user.getId())));
        vo.setEmployeeTenantCount(tenantEmployeeMapper.selectCount(new LambdaQueryWrapper<TenantEmployee>()
                .eq(TenantEmployee::getPlatformUserId, user.getId())));

        UnifiedWalletAccount unifiedWalletAccount = unifiedWalletAccountMapper.selectOne(new LambdaQueryWrapper<UnifiedWalletAccount>()
                .eq(UnifiedWalletAccount::getPlatformUserId, user.getId()));
        vo.setUnifiedWalletBalance(unifiedWalletAccount == null ? BigDecimal.ZERO : unifiedWalletAccount.getAvailableAmount());

        merchantWalletAccountMapper.selectCount(new LambdaQueryWrapper<MerchantWalletAccount>()
                .eq(MerchantWalletAccount::getPlatformUserId, user.getId()));
        memberPointsAccountMapper.selectCount(new LambdaQueryWrapper<MemberPointsAccount>()
                .eq(MemberPointsAccount::getPlatformUserId, user.getId()));
        return vo;
    }

    /** 安全地将 SQL 聚合结果转为 Long，null 或非数字返回 0L */
    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    /** 安全地将 SQL 聚合结果转为 BigDecimal，null 返回 ZERO */
    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return BigDecimal.ZERO;
    }

    /** 安全获取 selectMaps 结果的第一行，空结果返回空 Map */
    private Map<String, Object> firstOrEmpty(List<Map<String, Object>> list) {
        return (list != null && !list.isEmpty()) ? list.get(0) : java.util.Collections.emptyMap();
    }

    /** 构建提现审批 DTO */
    private WithdrawalApproveDTO buildWithdrawalApproveDTO(Long withdrawalId, boolean approved, String reason) {
        WithdrawalApproveDTO dto = new WithdrawalApproveDTO();
        dto.setWithdrawalId(withdrawalId);
        dto.setApproved(approved);
        dto.setRejectReason(reason);
        return dto;
    }
}
