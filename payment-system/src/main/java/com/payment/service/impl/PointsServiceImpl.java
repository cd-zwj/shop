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
 * 积分服务实现类，负责多租户场景下积分体系的完整业务处理。
 * <p>核心职责包括：</p>
 * <ul>
 *     <li>积分规则管理：查询、创建及更新各租户的积分获取规则</li>
 *     <li>积分发放与扣减：基于乐观锁重试机制保障并发安全，记录积分变动明细</li>
 *     <li>积分兑换商品：管理兑换商品的增删改查，支持积分兑换下单并扣减库存</li>
 *     <li>退款积分回退：订单退款时将已消耗积分退回用户账户</li>
 * </ul>
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
    
    /**
     * 查询指定租户的积分规则（状态为启用的规则）
     *
     * @param tenantId 租户ID
     * @return 积分规则实体，不存在时返回null
     */
    @Override
    public PointsRule getPointsRule(Long tenantId) {
        return pointsRuleMapper.selectOne(
                new LambdaQueryWrapper<PointsRule>()
                        .eq(PointsRule::getTenantId, tenantId)
                        .eq(PointsRule::getStatus, 1)
        );
    }
    
    /**
     * 设置当前租户的积分规则，已存在则更新，不存在则创建
     *
     * @param dto 积分规则配置DTO，包含积分比例和启用状态
     */
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
            existRule.setPointsAmount(dto.getPointsRatio());
            existRule.setStatus(dto.getEnabled());
            existRule.setUpdateTime(LocalDateTime.now());
            pointsRuleMapper.updateById(existRule);
            log.info("更新积分规则，tenantId={}, pointsAmount={}, status={}",
                    tenantId, dto.getPointsRatio(), dto.getEnabled());
        } else {
            // 创建规则
            PointsRule rule = new PointsRule();
            rule.setTenantId(tenantId);
            rule.setRuleType("PAYMENT");
            rule.setPointsAmount(dto.getPointsRatio());
            rule.setStatus(dto.getEnabled());
            rule.setCreateTime(LocalDateTime.now());
            rule.setUpdateTime(LocalDateTime.now());
            pointsRuleMapper.insert(rule);
            log.info("创建积分规则，tenantId={}, pointsAmount={}, status={}",
                    tenantId, dto.getPointsRatio(), dto.getEnabled());
        }
    }
    
    /**
     * 根据消费金额和租户积分规则计算应获得的积分数
     *
     * @param amount   消费金额
     * @param tenantId 租户ID
     * @return 计算得到的积分数，规则不存在或未启用时返回0
     */
    @Override
    public Integer calculatePoints(BigDecimal amount, Long tenantId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        
        // 获取积分规则
        PointsRule rule = getPointsRule(tenantId);
        if (rule == null || rule.getStatus() == 0) {
            return 0;
        }

        // 计算积分：按 pointsAmount（DDL 模型）
        int points = rule.getPointsAmount() != null ? rule.getPointsAmount() : 0;
        log.info("计算积分，tenantId={}, amount={}, pointsAmount={}, points={}",
                tenantId, amount, rule.getPointsAmount(), points);
        
        return points;
    }
    
    /**
     * 向用户发放积分，采用乐观锁重试机制保障并发安全。
     * 若用户积分记录不存在则自动创建，并发创建冲突时回退为重试更新。
     *
     * @param userId  用户ID
     * @param points  发放的积分数（必须大于0）
     * @param reason  发放原因说明
     * @param orderNo 关联订单号，可为null
     */
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
        log.setChangePoints(points);
        log.setPointsAfter(userPoints.getPoints());
        log.setChangeType("EARN");
        log.setRemark(reason);
        log.setOrderNo(orderNo);
        log.setCreateTime(LocalDateTime.now());
        pointsLogMapper.insert(log);
        
        PointsServiceImpl.log.info("发放积分成功，userId={}, points={}, balance={}, reason={}",
                userId, points, userPoints.getPoints(), reason);
    }
    
    /**
     * 扣减用户积分，采用乐观锁重试机制保障并发安全。
     * 扣减前校验积分余额是否充足，不足时抛出业务异常。
     *
     * @param userId 用户ID
     * @param points 扣减的积分数（必须大于0）
     * @param reason 扣减原因说明
     * @throws BusinessException 积分余额不足或操作冲突时抛出
     */
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
        log.setChangePoints(-points);
        log.setPointsAfter(userPoints.getPoints());
        log.setChangeType("USE");
        log.setRemark(reason);
        log.setCreateTime(LocalDateTime.now());
        pointsLogMapper.insert(log);
        
        PointsServiceImpl.log.info("扣减积分成功，userId={}, points={}, balance={}, reason={}",
                userId, points, userPoints.getPoints(), reason);
    }
    
    /**
     * 查询用户在指定租户下的当前积分余额
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 当前积分余额，积分记录不存在时返回0
     */
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

        // 检查是否已存在兑换配置
        ExchangeProduct existProduct = exchangeProductMapper.selectOne(
                new LambdaQueryWrapper<ExchangeProduct>()
                        .eq(ExchangeProduct::getProductName, dto.getProductName())
                        .eq(ExchangeProduct::getTenantId, tenantId)
                        .eq(ExchangeProduct::getDeleted, 0)
        );

        if (existProduct != null) {
            throw new BusinessException("该商品已配置为兑换商品");
        }

        // 创建兑换商品
        ExchangeProduct exchangeProduct = new ExchangeProduct();
        exchangeProduct.setTenantId(tenantId);
        exchangeProduct.setProductName(dto.getProductName());
        exchangeProduct.setProductImage(dto.getProductImage());
        exchangeProduct.setPointsRequired(dto.getPointsRequired());
        exchangeProduct.setStock(dto.getStock());
        exchangeProduct.setExchangeLimit(dto.getExchangeLimit());
        exchangeProduct.setDescription(dto.getDescription());
        exchangeProduct.setStatus(dto.getStatus());
        exchangeProduct.setSortOrder(dto.getSortOrder());
        exchangeProduct.setDeleted(0);
        exchangeProduct.setCreateTime(LocalDateTime.now());
        exchangeProduct.setUpdateTime(LocalDateTime.now());
        exchangeProductMapper.insert(exchangeProduct);

        log.info("创建积分兑换商品，tenantId={}, productName={}, pointsRequired={}",
                tenantId, dto.getProductName(), dto.getPointsRequired());
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
        exchangeProduct.setProductName(dto.getProductName());
        exchangeProduct.setProductImage(dto.getProductImage());
        exchangeProduct.setPointsRequired(dto.getPointsRequired());
        exchangeProduct.setStock(dto.getStock());
        exchangeProduct.setExchangeLimit(dto.getExchangeLimit());
        exchangeProduct.setDescription(dto.getDescription());
        exchangeProduct.setStatus(dto.getStatus());
        exchangeProduct.setSortOrder(dto.getSortOrder());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundPoints(Long userId, Long tenantId, Integer points, String orderNo, String reason) {
        if (points == null || points <= 0) {
            log.warn("退款回退积分数量无效，userId={}, points={}", userId, points);
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
            // 积分记录不存在，创建一条新的（退款回退不计入累计获得）
            userPoints = new UserPoints();
            userPoints.setUserId(userId);
            userPoints.setTenantId(tenantId);
            userPoints.setPoints(points);
            userPoints.setTotalEarned(0);
            userPoints.setTotalUsed(0);
            userPoints.setDeleted(0);
            userPoints.setCreateTime(LocalDateTime.now());
            userPoints.setUpdateTime(LocalDateTime.now());
            userPointsMapper.insert(userPoints);
        } else {
            // 回退积分（乐观锁重试）
            for (int attempt = 0; attempt < 3; attempt++) {
                userPoints.setPoints(userPoints.getPoints() + points);
                userPoints.setUpdateTime(LocalDateTime.now());
                if (userPointsMapper.updateById(userPoints) > 0) break;
                userPoints = userPointsMapper.selectById(userPoints.getId());
                if (userPoints == null) {
                    throw new BusinessException("积分账户不存在");
                }
                if (attempt == 2) throw new BusinessException("操作冲突，请重试");
            }
        }

        // 记录积分明细
        PointsLog pointsLog = new PointsLog();
        pointsLog.setTenantId(tenantId);
        pointsLog.setUserId(userId);
        pointsLog.setChangePoints(points);
        pointsLog.setPointsAfter(userPoints.getPoints());
        pointsLog.setChangeType("EXPIRE");
        pointsLog.setRemark(reason);
        pointsLog.setOrderNo(orderNo);
        pointsLog.setCreateTime(LocalDateTime.now());
        pointsLogMapper.insert(pointsLog);

        log.info("退款回退积分成功，userId={}, points={}, balance={}, orderNo={}, reason={}",
                userId, points, userPoints.getPoints(), orderNo, reason);
    }
}
