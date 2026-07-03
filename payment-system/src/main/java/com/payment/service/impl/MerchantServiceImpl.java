package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payment.common.BusinessException;
import com.payment.dto.MerchantDTO;
import com.payment.dto.MerchantDetailVO;
import com.payment.dto.MerchantQueryDTO;
import com.payment.entity.PaymentOrder;
import com.payment.entity.Product;
import com.payment.entity.Tenant;
import com.payment.mapper.PaymentOrderMapper;
import com.payment.mapper.ProductMapper;
import com.payment.mapper.TenantMapper;
import com.payment.service.MerchantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家管理服务实现类。
 * <p>
 * 提供商家（租户）全生命周期管理能力，包括：商家创建与信息更新、
 * 启用/禁用状态切换、分页列表查询、商家详情（含商品数/订单数/销售额聚合）、
 * 平台级数据看板统计以及商家注册趋势与销售趋势的月度报表查询。
 * </p>
 */
@Slf4j
@Service
public class MerchantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements MerchantService {
    
    @Autowired
    private TenantMapper tenantMapper;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private PaymentOrderMapper paymentOrderMapper;
    
    @Autowired
    private com.payment.mapper.WithdrawalMapper withdrawalMapper;
    
    /**
     * 创建新商家（租户）。
     * <p>
     * 校验租户编码唯一性后，初始化商家状态为启用（status=1），
     * 并设置创建/更新时间后持久化到数据库。
     * </p>
     *
     * @param dto 商家创建参数，包含租户编码、名称等基本信息
     * @return 创建成功的租户实体
     * @throws BusinessException 若租户编码已存在则抛出 400 异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tenant createMerchant(MerchantDTO dto) {
        log.info("创建商家，tenantCode: {}, name: {}", dto.getTenantCode(), dto.getName());
        
        // 检查租户编码是否已存在
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tenant::getTenantCode, dto.getTenantCode());
        wrapper.eq(Tenant::getDeleted, 0);
        Tenant existingTenant = tenantMapper.selectOne(wrapper);
        if (existingTenant != null) {
            throw new BusinessException(400, "租户编码已存在");
        }
        
        // 创建租户
        Tenant tenant = new Tenant();
        BeanUtils.copyProperties(dto, tenant);
        tenant.setStatus(1); // 默认启用
        tenant.setDeleted(0);
        tenant.setCreateTime(LocalDateTime.now());
        tenant.setUpdateTime(LocalDateTime.now());
        
        tenantMapper.insert(tenant);
        
        log.info("商家创建成功，tenantId: {}", tenant.getId());
        return tenant;
    }
    
    /**
     * 更新商家基本信息。
     * <p>
     * 若修改了租户编码，会额外校验新编码的唯一性（排除自身）。
     * 商家不存在或已逻辑删除时抛出异常。
     * </p>
     *
     * @param tenantId 待更新的商家租户 ID
     * @param dto      更新参数，包含新的商家名称、编码、联系方式等
     * @throws BusinessException 若商家不存在抛出 404，若新编码冲突抛出 400
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMerchant(Long tenantId, MerchantDTO dto) {
        log.info("更新商家信息，tenantId: {}", tenantId);
        
        // 查询商家是否存在
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getDeleted() == 1) {
            throw new BusinessException(404, "商家不存在");
        }
        
        // 如果修改了租户编码，检查新编码是否已存在
        if (!tenant.getTenantCode().equals(dto.getTenantCode())) {
            LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Tenant::getTenantCode, dto.getTenantCode());
            wrapper.eq(Tenant::getDeleted, 0);
            wrapper.ne(Tenant::getId, tenantId);
            Tenant existingTenant = tenantMapper.selectOne(wrapper);
            if (existingTenant != null) {
                throw new BusinessException(400, "租户编码已存在");
            }
        }
        
        // 更新商家信息
        BeanUtils.copyProperties(dto, tenant);
        tenant.setUpdateTime(LocalDateTime.now());
        
        tenantMapper.updateById(tenant);
        
        log.info("商家信息更新成功，tenantId: {}", tenantId);
    }
    
    /**
     * 启用商家。
     * <p>
     * 将指定商家的状态设置为启用（status=1），使其可以正常开展业务。
     * </p>
     *
     * @param tenantId 待启用的商家租户 ID
     * @throws BusinessException 若商家不存在或已逻辑删除则抛出 404 异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableMerchant(Long tenantId) {
        log.info("启用商家，tenantId: {}", tenantId);
        
        // 查询商家是否存在
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getDeleted() == 1) {
            throw new BusinessException(404, "商家不存在");
        }
        
        // 更新状态为启用
        tenant.setStatus(1);
        tenant.setUpdateTime(LocalDateTime.now());
        
        tenantMapper.updateById(tenant);
        
        log.info("商家启用成功，tenantId: {}", tenantId);
    }
    
    /**
     * 禁用商家。
     * <p>
     * 将指定商家的状态设置为禁用（status=0），暂停其业务能力。
     * </p>
     *
     * @param tenantId 待禁用的商家租户 ID
     * @throws BusinessException 若商家不存在或已逻辑删除则抛出 404 异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableMerchant(Long tenantId) {
        log.info("禁用商家，tenantId: {}", tenantId);
        
        // 查询商家是否存在
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getDeleted() == 1) {
            throw new BusinessException(404, "商家不存在");
        }
        
        // 更新状态为禁用
        tenant.setStatus(0);
        tenant.setUpdateTime(LocalDateTime.now());
        
        tenantMapper.updateById(tenant);
        
        log.info("商家禁用成功，tenantId: {}", tenantId);
    }
    
    /**
     * 分页查询商家列表。
     * <p>
     * 支持按商家名称模糊查询和状态精确过滤，结果按创建时间倒序排列。
     * 自动排除已逻辑删除的商家。
     * </p>
     *
     * @param query 查询条件，包含名称、状态、页码、每页大小
     * @return 分页结果，包含商家列表和总数
     */
    @Override
    public Page<Tenant> listMerchants(MerchantQueryDTO query) {
        log.info("查询商家列表，query: {}", query);
        
        // 构建查询条件
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tenant::getDeleted, 0);
        
        // 商家名称模糊查询
        if (query.getName() != null && !query.getName().isEmpty()) {
            wrapper.like(Tenant::getName, query.getName());
        }
        
        // 状态查询
        if (query.getStatus() != null) {
            wrapper.eq(Tenant::getStatus, query.getStatus());
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(Tenant::getCreateTime);
        
        // 分页查询
        Page<Tenant> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<Tenant> result = tenantMapper.selectPage(page, wrapper);
        
        log.info("商家列表查询成功，总数: {}", result.getTotal());
        return result;
    }
    
    /**
     * 查询商家详情。
     * <p>
     * 返回商家基本信息，并聚合统计该商家的商品数量、订单数量及已支付订单的总销售额。
     * </p>
     *
     * @param tenantId 商家租户 ID
     * @return 商家详情 VO，包含基本信息及商品数、订单数、总销售额
     * @throws BusinessException 若商家不存在或已逻辑删除则抛出 404 异常
     */
    @Override
    public MerchantDetailVO getMerchantDetail(Long tenantId) {
        log.info("查询商家详情，tenantId: {}", tenantId);
        
        // 查询商家基本信息
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getDeleted() == 1) {
            throw new BusinessException(404, "商家不存在");
        }
        
        // 构建详情VO
        MerchantDetailVO detailVO = new MerchantDetailVO();
        detailVO.setId(tenant.getId());
        detailVO.setTenantCode(tenant.getTenantCode());
        detailVO.setName(tenant.getName());
        detailVO.setContactName(tenant.getContact());
        detailVO.setContactPhone(tenant.getPhone());
        detailVO.setAddress(tenant.getAddress());
        detailVO.setStatus(tenant.getStatus());
        detailVO.setCreateTime(tenant.getCreateTime());
        
        // 查询商品数量
        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.eq(Product::getTenantId, tenantId);
        productWrapper.eq(Product::getDeleted, 0);
        Long productCount = productMapper.selectCount(productWrapper);
        detailVO.setProductCount(productCount);
        
        // 查询订单数量和销售额
        LambdaQueryWrapper<PaymentOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(PaymentOrder::getTenantId, tenantId);
        orderWrapper.eq(PaymentOrder::getDeleted, 0);
        Long orderCount = paymentOrderMapper.selectCount(orderWrapper);
        detailVO.setOrderCount(orderCount);
        
        // 计算销售额（已支付订单）
        LambdaQueryWrapper<PaymentOrder> salesWrapper = new LambdaQueryWrapper<>();
        salesWrapper.eq(PaymentOrder::getTenantId, tenantId);
        salesWrapper.eq(PaymentOrder::getPayStatus, "SUCCESS"); // 已支付
        salesWrapper.eq(PaymentOrder::getDeleted, 0);
        salesWrapper.select(PaymentOrder::getPayAmount);
        
        BigDecimal totalSales = paymentOrderMapper.selectList(salesWrapper).stream()
                .map(PaymentOrder::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        detailVO.setTotalSales(totalSales);
        
        log.info("商家详情查询成功，tenantId: {}, productCount: {}, orderCount: {}, totalSales: {}", 
                tenantId, productCount, orderCount, totalSales);
        
        return detailVO;
    }

    /**
     * 获取平台级数据看板统计。
     * <p>
     * 汇总以下关键指标：商家总数、启用商家数、平台总销售额（已支付订单）、
     * 待审核提现申请数量。用于管理端首页数据概览展示。
     * </p>
     *
     * @return 统计数据 Map，包含 totalMerchants、activeMerchants、totalSales、pendingWithdrawals
     */
    @Override
    public java.util.Map<String, Object> getDashboardStats() {
        log.info("获取平台数据概览");
        
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        // 查询商家总数
        LambdaQueryWrapper<Tenant> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(Tenant::getDeleted, 0);
        Long totalMerchants = tenantMapper.selectCount(totalWrapper);
        stats.put("totalMerchants", totalMerchants);
        
        // 查询启用商家数
        LambdaQueryWrapper<Tenant> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(Tenant::getDeleted, 0);
        activeWrapper.eq(Tenant::getStatus, 1);
        Long activeMerchants = tenantMapper.selectCount(activeWrapper);
        stats.put("activeMerchants", activeMerchants);
        
        // 查询平台总销售额（所有已支付订单）
        LambdaQueryWrapper<PaymentOrder> salesWrapper = new LambdaQueryWrapper<>();
        salesWrapper.eq(PaymentOrder::getPayStatus, "SUCCESS"); // 已支付
        salesWrapper.eq(PaymentOrder::getDeleted, 0);
        salesWrapper.select(PaymentOrder::getPayAmount);
        
        BigDecimal totalSales = paymentOrderMapper.selectList(salesWrapper).stream()
                .map(PaymentOrder::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalSales", totalSales.toString());
        
        // 查询待审核提现数量
        LambdaQueryWrapper<com.payment.entity.Withdrawal> withdrawalWrapper = new LambdaQueryWrapper<>();
        withdrawalWrapper.eq(com.payment.entity.Withdrawal::getStatus, 0); // 待审核
        withdrawalWrapper.eq(com.payment.entity.Withdrawal::getDeleted, 0);
        Long pendingWithdrawals = withdrawalMapper.selectCount(withdrawalWrapper);
        stats.put("pendingWithdrawals", pendingWithdrawals);
        
        log.info("平台数据概览：totalMerchants={}, activeMerchants={}, totalSales={}", 
                totalMerchants, activeMerchants, totalSales);
        
        return stats;
    }
    
    @Override
    public java.util.Map<String, Object> getMerchantTrend() {
        log.info("获取商家注册趋势");

        java.util.Map<String, Object> data = new java.util.HashMap<>();

        // 单条 GROUP BY 查询替代逐月循环（修复 N 查询问题）
        LocalDateTime endExclusive = LocalDateTime.now().plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startInclusive = endExclusive.minusMonths(6);

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Tenant> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Tenant>()
                        .select("DATE_FORMAT(create_time, '%Y-%m') AS month", "COUNT(*) AS cnt")
                        .eq("deleted", 0)
                        .ge("create_time", startInclusive)
                        .lt("create_time", endExclusive)
                        .groupBy("DATE_FORMAT(create_time, '%Y-%m')")
                        .orderByAsc("month");

        java.util.List<java.util.Map<String, Object>> rows = tenantMapper.selectMaps(qw);
        java.util.Map<String, Long> countMap = new java.util.LinkedHashMap<>();
        for (java.util.Map<String, Object> row : rows) {
            String month = String.valueOf(row.get("month"));
            long cnt = row.get("cnt") instanceof Number ? ((Number) row.get("cnt")).longValue() : 0L;
            countMap.put(month, cnt);
        }

        java.util.List<String> dates = new java.util.ArrayList<>();
        java.util.List<Integer> counts = new java.util.ArrayList<>();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 5; i >= 0; i--) {
            String monthStr = LocalDateTime.now().minusMonths(i).format(fmt);
            dates.add(monthStr);
            counts.add(countMap.getOrDefault(monthStr, 0L).intValue());
        }

        data.put("dates", dates);
        data.put("counts", counts);
        return data;
    }

    @Override
    public java.util.Map<String, Object> getSalesTrend() {
        log.info("获取平台销售趋势");

        java.util.Map<String, Object> data = new java.util.HashMap<>();

        // 单条 GROUP BY 查询替代逐月循环（修复 N 查询 + 全表加载问题）
        LocalDateTime endExclusive = LocalDateTime.now().plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startInclusive = endExclusive.minusMonths(6);

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PaymentOrder> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PaymentOrder>()
                        .select("DATE_FORMAT(pay_time, '%Y-%m') AS month", "COALESCE(SUM(pay_amount), 0) AS total")
                        .eq("pay_status", "SUCCESS")
                        .eq("deleted", 0)
                        .ge("pay_time", startInclusive)
                        .lt("pay_time", endExclusive)
                        .groupBy("DATE_FORMAT(pay_time, '%Y-%m')")
                        .orderByAsc("month");

        java.util.List<java.util.Map<String, Object>> rows = paymentOrderMapper.selectMaps(qw);
        java.util.Map<String, Double> amountMap = new java.util.LinkedHashMap<>();
        for (java.util.Map<String, Object> row : rows) {
            String month = String.valueOf(row.get("month"));
            double amt = row.get("total") instanceof Number ? ((Number) row.get("total")).doubleValue() : 0.0;
            amountMap.put(month, amt);
        }

        java.util.List<String> dates = new java.util.ArrayList<>();
        java.util.List<Double> amounts = new java.util.ArrayList<>();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 5; i >= 0; i--) {
            String monthStr = LocalDateTime.now().minusMonths(i).format(fmt);
            dates.add(monthStr);
            amounts.add(amountMap.getOrDefault(monthStr, 0.0));
        }

        data.put("dates", dates);
        data.put("amounts", amounts);
        return data;
    }
}
