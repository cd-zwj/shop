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
 * 商家管理服务实现
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
        
        // 查询最近6个月的商家注册数据
        java.util.List<String> dates = new java.util.ArrayList<>();
        java.util.List<Integer> counts = new java.util.ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            
            // 格式化月份
            String monthStr = monthStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            dates.add(monthStr);
            
            // 查询该月注册的商家数量
            LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Tenant::getDeleted, 0);
            wrapper.ge(Tenant::getCreateTime, monthStart);
            wrapper.lt(Tenant::getCreateTime, monthEnd);
            Long count = tenantMapper.selectCount(wrapper);
            counts.add(count.intValue());
        }
        
        data.put("dates", dates);
        data.put("counts", counts);
        
        log.info("商家注册趋势：dates={}, counts={}", dates, counts);
        
        return data;
    }
    
    @Override
    public java.util.Map<String, Object> getSalesTrend() {
        log.info("获取平台销售趋势");
        
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        
        // 查询最近6个月的销售数据
        java.util.List<String> dates = new java.util.ArrayList<>();
        java.util.List<Double> amounts = new java.util.ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            
            // 格式化月份
            String monthStr = monthStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            dates.add(monthStr);
            
            // 查询该月的销售额
            LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PaymentOrder::getPayStatus, "SUCCESS"); // 已支付
            wrapper.eq(PaymentOrder::getDeleted, 0);
            wrapper.ge(PaymentOrder::getPayTime, monthStart);
            wrapper.lt(PaymentOrder::getPayTime, monthEnd);
            wrapper.select(PaymentOrder::getPayAmount);
            
            BigDecimal monthSales = paymentOrderMapper.selectList(wrapper).stream()
                    .map(PaymentOrder::getPayAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            amounts.add(monthSales.doubleValue());
        }
        
        data.put("dates", dates);
        data.put("amounts", amounts);
        
        log.info("平台销售趋势：dates={}, amounts={}", dates, amounts);
        
        return data;
    }
}
