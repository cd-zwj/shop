package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
import com.payment.dto.ExchangeProductDTO;
import com.payment.dto.PointsRuleDTO;
import com.payment.entity.*;
import com.payment.mapper.*;
import com.payment.service.PointsService;
import com.payment.util.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 积分服务实现类
 */
@Slf4j
@Service
public class PointsServiceImpl implements PointsService {
    
    @Autowired
    private PointsRuleMapper pointsRuleMapper;
    
    @Autowired
    private PointsLogMapper pointsLogMapper;
    
    @Autowired
    private UserPointsMapper userPointsMapper;
    
    @Autowired
    private ExchangeProductMapper exchangeProductMapper;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Override
    public PointsRule getPointsRule(Long tenantId) {
        return pointsRuleMapper.selectOne(
                new LambdaQueryWrapper<PointsRule>()
                        .eq(PointsRule::getTenantId, tenantId)
                        .eq(PointsRule::getDeleted, 0)
        );
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPointsRule(PointsRuleDTO dto) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        
        // 查询是否已存在规则
        PointsRule existRule = getPointsRule(tenantId);
        
        if (existRule != null) {
            // 更新规则
            existRule.setPointsRatio(dto.getPointsRatio());
            existRule.setEnabled(dto.getEnabled());
            existRule.setUpdateTime(LocalDateTime.now());
            pointsRuleMapper.updateById(existRule);
            log.info("更新积分规则，tenantId={}, pointsRatio={}, enabled={}", 
                    tenantId, dto.getPointsRatio(), dto.getEnabled());
        } else {
            // 创建规则
            PointsRule rule = new PointsRule();
            rule.setTenantId(tenantId);
            rule.setPointsRatio(dto.getPointsRatio());
            rule.setEnabled(dto.getEnabled());
            rule.setDeleted(0);
            rule.setCreateTime(LocalDateTime.now());
            rule.setUpdateTime(LocalDateTime.now());
            pointsRuleMapper.insert(rule);
            log.info("创建积分规则，tenantId={}, pointsRatio={}, enabled={}", 
                    tenantId, dto.getPointsRatio(), dto.getEnabled());
        }
    }
    
    @Override
    public Integer calculatePoints(BigDecimal amount, Long tenantId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        
        // 获取积分规则
        PointsRule rule = getPointsRule(tenantId);
        if (rule == null || rule.getEnabled() == 0) {
            return 0;
        }
        
        // 计算积分：订单金额 * 积分比例
        int points = amount.intValue() * rule.getPointsRatio();
        log.info("计算积分，tenantId={}, amount={}, pointsRatio={}, points={}", 
                tenantId, amount, rule.getPointsRatio(), points);
        
        return points;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantPoints(Long userId, Integer points, String reason, String orderNo) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        
        if (points == null || points <= 0) {
            log.warn("积分数量无效，userId={}, points={}", userId, points);
            return;
        }
        
        // 查询或创建用户积分记录
        UserPoints userPoints = userPointsMapper.selectOne(
                new LambdaQueryWrapper<UserPoints>()
                        .eq(UserPoints::getUserId, userId)
                        .eq(UserPoints::getTenantId, tenantId)
                        .eq(UserPoints::getDeleted, 0)
        );
        
        if (userPoints == null) {
            // 创建用户积分记录
            userPoints = new UserPoints();
            userPoints.setUserId(userId);
            userPoints.setTenantId(tenantId);
            userPoints.setPoints(points);
            userPoints.setTotalEarned(points);
            userPoints.setTotalUsed(0);
            userPoints.setDeleted(0);
            userPoints.setCreateTime(LocalDateTime.now());
            userPoints.setUpdateTime(LocalDateTime.now());
            try {
                userPointsMapper.insert(userPoints);
            } catch (Exception e) {
                // 并发创建时 DuplicateKeyException → 回退到重试更新
                log.warn("积分记录并发创建冲突，转为更新，userId={}", userId);
                userPoints = userPointsMapper.selectOne(
                        new LambdaQueryWrapper<UserPoints>()
                                .eq(UserPoints::getUserId, userId)
                                .eq(UserPoints::getTenantId, tenantId)
                                .eq(UserPoints::getDeleted, 0));
                if (userPoints == null) {
                    throw new BusinessException("创建积分记录失败");
                }
                for (int attempt = 0; attempt < 3; attempt++) {
                    userPoints.setPoints(userPoints.getPoints() + points);
                    userPoints.setTotalEarned(userPoints.getTotalEarned() + points);
                    userPoints.setUpdateTime(LocalDateTime.now());
                    if (userPointsMapper.updateById(userPoints) > 0) break;
                    userPoints = userPointsMapper.selectById(userPoints.getId());
                }
            }
        } else {
            // 更新用户积分（乐观锁重试）
            for (int attempt = 0; attempt < 3; attempt++) {
                userPoints.setPoints(userPoints.getPoints() + points);
                userPoints.setTotalEarned(userPoints.getTotalEarned() + points);
                userPoints.setUpdateTime(LocalDateTime.now());
                if (userPointsMapper.updateById(userPoints) > 0) break;
                userPoints = userPointsMapper.selectById(userPoints.getId());
                if (attempt == 2) throw new BusinessException("操作冲突，请重试");
            }
        }
        
        // 记录积分明细
        PointsLog log = new PointsLog();
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setPoints(points);
        log.setBalance(userPoints.getPoints());
        log.setType("GRANT");
        log.setReason(reason);
        log.setOrderNo(orderNo);
        log.setDeleted(0);
        log.setCreateTime(LocalDateTime.now());
        pointsLogMapper.insert(log);
        
        PointsServiceImpl.log.info("发放积分成功，userId={}, points={}, balance={}, reason={}",
                userId, points, userPoints.getPoints(), reason);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductPoints(Long userId, Integer points, String reason) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        
        if (points == null || points <= 0) {
            throw new BusinessException("扣减积分数量必须大于0");
        }
        
        // 查询用户积分
        UserPoints userPoints = userPointsMapper.selectOne(
                new LambdaQueryWrapper<UserPoints>()
                        .eq(UserPoints::getUserId, userId)
                        .eq(UserPoints::getTenantId, tenantId)
                        .eq(UserPoints::getDeleted, 0)
        );
        
        if (userPoints == null || userPoints.getPoints() < points) {
            throw new BusinessException("积分余额不足");
        }
        
        // 扣减积分（乐观锁重试）
        for (int attempt = 0; attempt < 3; attempt++) {
            userPoints.setPoints(userPoints.getPoints() - points);
            userPoints.setTotalUsed(userPoints.getTotalUsed() + points);
            userPoints.setUpdateTime(LocalDateTime.now());
            if (userPointsMapper.updateById(userPoints) > 0) break;
            userPoints = userPointsMapper.selectById(userPoints.getId());
            if (userPoints == null || userPoints.getPoints() < points) {
                throw new BusinessException("积分余额不足");
            }
            if (attempt == 2) throw new BusinessException("操作冲突，请重试");
        }
        
        // 记录积分明细
        PointsLog log = new PointsLog();
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setPoints(-points);
        log.setBalance(userPoints.getPoints());
        log.setType("DEDUCT");
        log.setReason(reason);
        log.setDeleted(0);
        log.setCreateTime(LocalDateTime.now());
        pointsLogMapper.insert(log);
        
        PointsServiceImpl.log.info("扣减积分成功，userId={}, points={}, balance={}, reason={}",
                userId, points, userPoints.getPoints(), reason);
    }
    
    @Override
    public Integer getUserPoints(Long userId, Long tenantId) {
        UserPoints userPoints = userPointsMapper.selectOne(
                new LambdaQueryWrapper<UserPoints>()
                        .eq(UserPoints::getUserId, userId)
                        .eq(UserPoints::getTenantId, tenantId)
                        .eq(UserPoints::getDeleted, 0)
        );
        
        return userPoints != null ? userPoints.getPoints() : 0;
    }
    
    @Override
    public Page<PointsLog> listPointsLogs(Long userId, Long tenantId, Integer pageNum, Integer pageSize) {
        Page<PointsLog> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<PointsLog> wrapper = new LambdaQueryWrapper<PointsLog>()
                .eq(PointsLog::getUserId, userId)
                .eq(PointsLog::getTenantId, tenantId)
                .eq(PointsLog::getDeleted, 0)
                .orderByDesc(PointsLog::getCreateTime);
        
        return pointsLogMapper.selectPage(page, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String exchangeProduct(Long userId, Long exchangeProductId) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        
        // 查询兑换商品
        ExchangeProduct exchangeProduct = exchangeProductMapper.selectOne(
                new LambdaQueryWrapper<ExchangeProduct>()
                        .eq(ExchangeProduct::getId, exchangeProductId)
                        .eq(ExchangeProduct::getTenantId, tenantId)
                        .eq(ExchangeProduct::getDeleted, 0)
        );
        
        if (exchangeProduct == null) {
            throw new BusinessException("兑换商品不存在");
        }
        
        if (exchangeProduct.getStatus() == 0) {
            throw new BusinessException("兑换商品已下架");
        }
        
        if (exchangeProduct.getStock() <= 0) {
            throw new BusinessException("兑换商品库存不足");
        }
        
        // 查询用户积分
        Integer userPoints = getUserPoints(userId, tenantId);
        if (userPoints < exchangeProduct.getPointsRequired()) {
            throw new BusinessException("积分余额不足");
        }
        
        // 扣减积分
        deductPoints(userId, exchangeProduct.getPointsRequired(), "积分兑换商品");
        
        // 扣减兑换库存
        exchangeProduct.setStock(exchangeProduct.getStock() - 1);
        exchangeProduct.setUpdateTime(LocalDateTime.now());
        exchangeProductMapper.updateById(exchangeProduct);
        
        // 生成兑换订单号
        String orderNo = "EX" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
        
        log.info("积分兑换成功，userId={}, exchangeProductId={}, pointsRequired={}, orderNo={}", 
                userId, exchangeProductId, exchangeProduct.getPointsRequired(), orderNo);
        
        return orderNo;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setExchangeProduct(ExchangeProductDTO dto) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        
        // 验证商品是否存在
        Product product = productMapper.selectOne(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getId, dto.getProductId())
                        .eq(Product::getTenantId, tenantId)
                        .eq(Product::getDeleted, 0)
        );
        
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        
        // 检查是否已存在兑换配置
        ExchangeProduct existProduct = exchangeProductMapper.selectOne(
                new LambdaQueryWrapper<ExchangeProduct>()
                        .eq(ExchangeProduct::getProductId, dto.getProductId())
                        .eq(ExchangeProduct::getTenantId, tenantId)
                        .eq(ExchangeProduct::getDeleted, 0)
        );
        
        if (existProduct != null) {
            throw new BusinessException("该商品已配置为兑换商品");
        }
        
        // 创建兑换商品
        ExchangeProduct exchangeProduct = new ExchangeProduct();
        BeanUtils.copyProperties(dto, exchangeProduct);
        exchangeProduct.setTenantId(tenantId);
        exchangeProduct.setDeleted(0);
        exchangeProduct.setCreateTime(LocalDateTime.now());
        exchangeProduct.setUpdateTime(LocalDateTime.now());
        exchangeProductMapper.insert(exchangeProduct);
        
        log.info("创建积分兑换商品，tenantId={}, productId={}, pointsRequired={}", 
                tenantId, dto.getProductId(), dto.getPointsRequired());
    }
    
    @Override
    public List<ExchangeProduct> listExchangeProducts(Long tenantId) {
        return exchangeProductMapper.selectList(
                new LambdaQueryWrapper<ExchangeProduct>()
                        .eq(ExchangeProduct::getTenantId, tenantId)
                        .eq(ExchangeProduct::getDeleted, 0)
                        .orderByDesc(ExchangeProduct::getCreateTime)
        );
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateExchangeProduct(Long id, ExchangeProductDTO dto) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        
        // 查询兑换商品
        ExchangeProduct exchangeProduct = exchangeProductMapper.selectOne(
                new LambdaQueryWrapper<ExchangeProduct>()
                        .eq(ExchangeProduct::getId, id)
                        .eq(ExchangeProduct::getTenantId, tenantId)
                        .eq(ExchangeProduct::getDeleted, 0)
        );
        
        if (exchangeProduct == null) {
            throw new BusinessException("兑换商品不存在");
        }
        
        // 更新兑换商品
        exchangeProduct.setPointsRequired(dto.getPointsRequired());
        exchangeProduct.setStock(dto.getStock());
        exchangeProduct.setStatus(dto.getStatus());
        exchangeProduct.setUpdateTime(LocalDateTime.now());
        exchangeProductMapper.updateById(exchangeProduct);
        
        log.info("更新积分兑换商品，id={}, pointsRequired={}, stock={}, status={}", 
                id, dto.getPointsRequired(), dto.getStock(), dto.getStatus());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExchangeProduct(Long id) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("租户信息不存在");
        }
        
        // 查询兑换商品
        ExchangeProduct exchangeProduct = exchangeProductMapper.selectOne(
                new LambdaQueryWrapper<ExchangeProduct>()
                        .eq(ExchangeProduct::getId, id)
                        .eq(ExchangeProduct::getTenantId, tenantId)
                        .eq(ExchangeProduct::getDeleted, 0)
        );
        
        if (exchangeProduct == null) {
            throw new BusinessException("兑换商品不存在");
        }
        
        // 软删除
        exchangeProduct.setDeleted(1);
        exchangeProduct.setUpdateTime(LocalDateTime.now());
        exchangeProductMapper.updateById(exchangeProduct);
        
        log.info("删除积分兑换商品，id={}", id);
    }
}
