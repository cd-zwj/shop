package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.TenantContextHolder;
import com.payment.dto.RechargeRuleDTO;
import com.payment.entity.BalanceLog;
import com.payment.entity.RechargeOrder;
import com.payment.entity.RechargeRule;
import com.payment.entity.UserBalance;
import com.payment.mapper.BalanceLogMapper;
import com.payment.mapper.RechargeOrderMapper;
import com.payment.mapper.RechargeRuleMapper;
import com.payment.mapper.UserBalanceMapper;
import com.payment.service.RechargeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 充值服务实现
 */
@Slf4j
@Service
public class RechargeServiceImpl implements RechargeService {
    
    @Resource
    private RechargeRuleMapper rechargeRuleMapper;
    
    @Resource
    private RechargeOrderMapper rechargeOrderMapper;
    
    @Resource
    private UserBalanceMapper userBalanceMapper;
    
    @Resource
    private BalanceLogMapper balanceLogMapper;
    
    @Override
    public List<RechargeRule> getRechargeRules(Long tenantId) {
        LambdaQueryWrapper<RechargeRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeRule::getTenantId, tenantId)
                .eq(RechargeRule::getEnabled, 1)
                .eq(RechargeRule::getDeleted, 0)
                .orderByAsc(RechargeRule::getSortOrder);
        return rechargeRuleMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setRechargeRules(List<RechargeRuleDTO> rules) {
        Long tenantId = TenantContextHolder.getTenantId();
        
        // 删除旧规则（软删除）
        LambdaQueryWrapper<RechargeRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeRule::getTenantId, tenantId);
        List<RechargeRule> oldRules = rechargeRuleMapper.selectList(wrapper);
        for (RechargeRule oldRule : oldRules) {
            oldRule.setDeleted(1);
            rechargeRuleMapper.updateById(oldRule);
        }
        
        // 添加新规则
        for (int i = 0; i < rules.size(); i++) {
            RechargeRuleDTO dto = rules.get(i);
            RechargeRule rule = new RechargeRule();
            BeanUtils.copyProperties(dto, rule);
            rule.setTenantId(tenantId);
            rule.setSortOrder(i);
            rule.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : 1);
            rule.setDeleted(0);
            rule.setCreateTime(LocalDateTime.now());
            rule.setUpdateTime(LocalDateTime.now());
            rechargeRuleMapper.insert(rule);
        }
        
        log.info("商家 {} 设置充值规则成功，共 {} 条", tenantId, rules.size());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeOrder createRechargeOrder(Long userId, Long ruleId) {
        Long tenantId = TenantContextHolder.getTenantId();
        
        // 查询充值规则
        RechargeRule rule = rechargeRuleMapper.selectById(ruleId);
        if (rule == null || rule.getDeleted() == 1) {
            throw new RuntimeException("充值规则不存在");
        }
        if (!rule.getTenantId().equals(tenantId)) {
            throw new RuntimeException("充值规则不属于当前商家");
        }
        if (rule.getEnabled() == 0) {
            throw new RuntimeException("充值规则已禁用");
        }
        
        // 创建充值订单
        RechargeOrder order = new RechargeOrder();
        order.setTenantId(tenantId);
        order.setUserId(userId);
        order.setOrderNo("R" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8));
        order.setRuleId(ruleId);
        order.setRechargeAmount(rule.getRechargeAmount());
        order.setBonusAmount(rule.getBonusAmount());
        order.setTotalAmount(rule.getRechargeAmount().add(rule.getBonusAmount()));
        order.setPayStatus(0);
        order.setDeleted(0);
        order.setCreateTime(LocalDateTime.now());
        
        rechargeOrderMapper.insert(order);
        
        log.info("用户 {} 创建充值订单 {}，充值金额 {}，赠送金额 {}", 
                userId, order.getOrderNo(), order.getRechargeAmount(), order.getBonusAmount());
        
        return order;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRechargeCallback(String orderNo) {
        // 查询充值订单
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getOrderNo, orderNo)
                .eq(RechargeOrder::getDeleted, 0);
        RechargeOrder order = rechargeOrderMapper.selectOne(wrapper);
        
        if (order == null) {
            throw new RuntimeException("充值订单不存在");
        }
        
        if (order.getPayStatus() == 1) {
            log.warn("充值订单 {} 已支付，忽略回调", orderNo);
            return;
        }
        
        // 更新订单状态
        order.setPayStatus(1);
        order.setPayTime(LocalDateTime.now());
        rechargeOrderMapper.updateById(order);
        
        // 增加用户余额
        BigDecimal totalAmount = order.getRechargeAmount().add(order.getBonusAmount());
        addUserBalance(order.getUserId(), order.getTenantId(), totalAmount, "RECHARGE", "充值", orderNo);
        
        log.info("充值订单 {} 支付成功，用户 {} 余额增加 {}", orderNo, order.getUserId(), totalAmount);
    }
    
    @Override
    public BigDecimal getUserBalance(Long userId, Long tenantId) {
        LambdaQueryWrapper<UserBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBalance::getUserId, userId)
                .eq(UserBalance::getTenantId, tenantId)
                .eq(UserBalance::getDeleted, 0);
        UserBalance userBalance = userBalanceMapper.selectOne(wrapper);
        
        return userBalance != null ? userBalance.getBalance() : BigDecimal.ZERO;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payWithBalance(Long userId, Long tenantId, String orderNo, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("支付金额必须大于0");
        }
        
        // 查询用户余额
        LambdaQueryWrapper<UserBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBalance::getUserId, userId)
                .eq(UserBalance::getTenantId, tenantId)
                .eq(UserBalance::getDeleted, 0);
        UserBalance userBalance = userBalanceMapper.selectOne(wrapper);
        
        if (userBalance == null || userBalance.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足");
        }
        
        // 扣减余额
        userBalance.setBalance(userBalance.getBalance().subtract(amount));
        userBalance.setTotalConsume(userBalance.getTotalConsume().add(amount));
        userBalance.setUpdateTime(LocalDateTime.now());
        userBalanceMapper.updateById(userBalance);
        
        // 记录余额变动
        BalanceLog log = new BalanceLog();
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setAmount(amount.negate());
        log.setBalance(userBalance.getBalance());
        log.setType("CONSUME");
        log.setReason("订单支付");
        log.setOrderNo(orderNo);
        log.setDeleted(0);
        log.setCreateTime(LocalDateTime.now());
        balanceLogMapper.insert(log);
        
        RechargeServiceImpl.log.info("用户 {} 使用余额支付 {}，订单号 {}", userId, amount, orderNo);
    }
    
    @Override
    public Page<BalanceLog> listBalanceLogs(Long userId, Long tenantId, Integer pageNum, Integer pageSize) {
        Page<BalanceLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BalanceLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BalanceLog::getUserId, userId)
                .eq(BalanceLog::getTenantId, tenantId)
                .eq(BalanceLog::getDeleted, 0)
                .orderByDesc(BalanceLog::getCreateTime);
        return balanceLogMapper.selectPage(page, wrapper);
    }
    
    /**
     * 增加用户余额
     */
    private void addUserBalance(Long userId, Long tenantId, BigDecimal amount, String type, String reason, String orderNo) {
        // 查询用户余额
        LambdaQueryWrapper<UserBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBalance::getUserId, userId)
                .eq(UserBalance::getTenantId, tenantId)
                .eq(UserBalance::getDeleted, 0);
        UserBalance userBalance = userBalanceMapper.selectOne(wrapper);
        
        if (userBalance == null) {
            // 创建用户余额记录
            userBalance = new UserBalance();
            userBalance.setTenantId(tenantId);
            userBalance.setUserId(userId);
            userBalance.setBalance(amount);
            userBalance.setTotalRecharge(amount);
            userBalance.setTotalConsume(BigDecimal.ZERO);
            userBalance.setDeleted(0);
            userBalance.setCreateTime(LocalDateTime.now());
            userBalance.setUpdateTime(LocalDateTime.now());
            userBalanceMapper.insert(userBalance);
        } else {
            // 更新用户余额
            userBalance.setBalance(userBalance.getBalance().add(amount));
            userBalance.setTotalRecharge(userBalance.getTotalRecharge().add(amount));
            userBalance.setUpdateTime(LocalDateTime.now());
            userBalanceMapper.updateById(userBalance);
        }
        
        // 记录余额变动
        BalanceLog log = new BalanceLog();
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setAmount(amount);
        log.setBalance(userBalance.getBalance());
        log.setType(type);
        log.setReason(reason);
        log.setOrderNo(orderNo);
        log.setDeleted(0);
        log.setCreateTime(LocalDateTime.now());
        balanceLogMapper.insert(log);
    }
}
