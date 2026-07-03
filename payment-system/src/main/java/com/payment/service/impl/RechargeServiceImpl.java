package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.common.BusinessException;
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
import com.payment.config.PaymentConfig;
import com.payment.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 充值服务实现（旧版）。
 * <p>
 * 处理商户级充值规则管理、充值订单创建、支付回调、用户余额管理等业务。
 * 与 {@link WalletRechargeServiceImpl} 的区别在于：本实现操作的是旧版余额体系（user_balance 表），
 * 而 WalletRechargeServiceImpl 操作的是新版双钱包体系（unified_wallet / merchant_wallet）。
 * 充值订单超时通过 RabbitMQ 延迟队列自动取消。
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
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private PaymentConfig paymentConfig;
    
    /**
     * 获取指定商户的可用充值规则列表（按排序权重升序）。
     *
     * @param tenantId 租户 ID
     * @return 启用状态的充值规则列表
     */
    @Override
    public List<RechargeRule> getRechargeRules(Long tenantId) {
        LambdaQueryWrapper<RechargeRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeRule::getTenantId, tenantId)
                .eq(RechargeRule::getStatus, 1)
                .orderByAsc(RechargeRule::getSortOrder);
        return rechargeRuleMapper.selectList(wrapper);
    }
    
    /**
     * 批量替换当前商户的充值规则。
     * <p>
     * 先删除旧规则（物理删除），再按顺序插入新规则。
     *
     * @param rules 充值规则 DTO 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setRechargeRules(List<RechargeRuleDTO> rules) {
        Long tenantId = TenantContextHolder.getTenantId();
        
        // 删除旧规则（软删除） — DDL 无 deleted 列，直接物理删除
        LambdaQueryWrapper<RechargeRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeRule::getTenantId, tenantId);
        rechargeRuleMapper.delete(wrapper);
        
        // 添加新规则
        for (int i = 0; i < rules.size(); i++) {
            RechargeRuleDTO dto = rules.get(i);
            RechargeRule rule = new RechargeRule();
            BeanUtils.copyProperties(dto, rule);
            rule.setTenantId(tenantId);
            rule.setSortOrder(i);
            rule.setStatus(dto.getEnabled() != null ? dto.getEnabled() : 1);
            rule.setCreateTime(LocalDateTime.now());
            rule.setUpdateTime(LocalDateTime.now());
            rechargeRuleMapper.insert(rule);
        }
        
        log.info("商家 {} 设置充值规则成功，共 {} 条", tenantId, rules.size());
    }
    
    /**
     * 创建充值订单。
     * <p>
     * 校验充值规则合法性和所属租户后创建订单，并通过 RabbitMQ 延迟队列设置超时自动取消。
     *
     * @param userId  用户 ID
     * @param ruleId  充值规则 ID
     * @return 新创建的充值订单
     * @throws BusinessException 规则不存在、已禁用或不属于当前租户时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeOrder createRechargeOrder(Long userId, Long ruleId) {
        Long tenantId = TenantContextHolder.getTenantId();
        
        // 查询充值规则
        RechargeRule rule = rechargeRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BusinessException("充值规则不存在");
        }
        if (!rule.getTenantId().equals(tenantId)) {
            throw new BusinessException("充值规则不属于当前商家");
        }
        if (rule.getStatus() == 0) {
            throw new BusinessException("充值规则已禁用");
        }
        
        // 创建充值订单
        RechargeOrder order = new RechargeOrder();
        order.setTenantId(tenantId);
        order.setUserId(userId);
        order.setOrderNo("R" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8));
        order.setRechargeAmount(rule.getRechargeAmount());
        order.setGiftAmount(rule.getGiftAmount());
        order.setActualAmount(rule.getRechargeAmount().add(rule.getGiftAmount()));
        order.setPayStatus("PENDING");
        order.setCreateTime(LocalDateTime.now());
        
        rechargeOrderMapper.insert(order);
        
        // 发送延迟消息到队列，处理订单超时自动取消
        try {
            int timeoutMinutes = paymentConfig.getRechargeOrderTimeoutMinutes() != null ? 
                    paymentConfig.getRechargeOrderTimeoutMinutes() : 15;
            String ttl = String.valueOf(timeoutMinutes * 60 * 1000); // 毫秒
            
            rabbitTemplate.convertAndSend(RabbitMQConfig.RECHARGE_ORDER_DELAY_QUEUE, (Object) order.getOrderNo(), message -> {
                message.getMessageProperties().setExpiration(ttl);
                return message;
            });
            log.info("充值订单 {} 已发送延迟消息，超时时间 {} 分钟", order.getOrderNo(), timeoutMinutes);
        } catch (Exception e) {
            log.error("发送充值订单延迟消息失败：{}", order.getOrderNo(), e);
        }
        
        log.info("用户 {} 创建充值订单 {}，充值金额 {}，赠送金额 {}",
                userId, order.getOrderNo(), order.getRechargeAmount(), order.getGiftAmount());
        
        return order;
    }
    
    /**
     * 处理充值支付成功回调。
     * <p>
     * 采用 CAS 原子更新保证幂等：只有 pay_status 从 PENDING 更新为 SUCCESS 时才执行入账，
     * 重复回调自动忽略。入账金额 = 充值金额 + 赠送金额。
     *
     * @param orderNo 充值订单号
     * @throws BusinessException 订单不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRechargeCallback(String orderNo) {
        // 幂等：原子地将 pay_status 从 PENDING 更新为 SUCCESS，只有首个线程 affectedRows == 1
        int affected = rechargeOrderMapper.update(null, new LambdaUpdateWrapper<RechargeOrder>()
                .eq(RechargeOrder::getOrderNo, orderNo)
                .eq(RechargeOrder::getPayStatus, "PENDING")
                .set(RechargeOrder::getPayStatus, "SUCCESS")
                .set(RechargeOrder::getPayTime, LocalDateTime.now()));
        if (affected == 0) {
            RechargeOrder existing = rechargeOrderMapper.selectOne(new LambdaQueryWrapper<RechargeOrder>()
                    .eq(RechargeOrder::getOrderNo, orderNo));
            if (existing == null) {
                throw new BusinessException("充值订单不存在");
            }
            log.warn("充值订单 {} 已处理，忽略重复回调", orderNo);
            return;
        }

        // 查询订单用于入账
        RechargeOrder order = rechargeOrderMapper.selectOne(new LambdaQueryWrapper<RechargeOrder>()
                .eq(RechargeOrder::getOrderNo, orderNo));
        BigDecimal totalAmount = order.getRechargeAmount().add(order.getGiftAmount());
        addUserBalance(order.getUserId(), order.getTenantId(), totalAmount, "RECHARGE", "充值", orderNo);
        log.info("充值订单 {} 支付成功，用户 {} 余额增加 {}", orderNo, order.getUserId(), totalAmount);
    }
    
    /**
     * 查询用户在指定商户下的余额。
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return 余额金额，账户不存在时返回 0
     */
    @Override
    public BigDecimal getUserBalance(Long userId, Long tenantId) {
        LambdaQueryWrapper<UserBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBalance::getUserId, userId)
                .eq(UserBalance::getTenantId, tenantId)
                .eq(UserBalance::getDeleted, 0);
        UserBalance userBalance = userBalanceMapper.selectOne(wrapper);
        
        return userBalance != null ? userBalance.getBalance() : BigDecimal.ZERO;
    }
    
    /**
     * 使用余额支付订单。
     * <p>
     * 采用乐观锁 + 重试机制（最多 3 次）保障并发安全，
     * 成功后记录余额变动流水。
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @param orderNo  订单号
     * @param amount   支付金额，必须大于 0
     * @throws BusinessException 余额不足、金额不合法或重试耗尽时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payWithBalance(Long userId, Long tenantId, String orderNo, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("支付金额必须大于0");
        }
        
        // 查询用户余额
        LambdaQueryWrapper<UserBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBalance::getUserId, userId)
                .eq(UserBalance::getTenantId, tenantId)
                .eq(UserBalance::getDeleted, 0);
        UserBalance userBalance = userBalanceMapper.selectOne(wrapper);
        
        if (userBalance == null || userBalance.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("余额不足");
        }
        
        // 扣减余额（乐观锁重试）
        for (int attempt = 0; attempt < 3; attempt++) {
            userBalance.setBalance(userBalance.getBalance().subtract(amount));
            userBalance.setTotalConsume(userBalance.getTotalConsume().add(amount));
            userBalance.setUpdateTime(LocalDateTime.now());
            if (userBalanceMapper.updateById(userBalance) > 0) break;
            userBalance = userBalanceMapper.selectOne(new LambdaQueryWrapper<UserBalance>()
                    .eq(UserBalance::getUserId, userId)
                    .eq(UserBalance::getTenantId, tenantId)
                    .eq(UserBalance::getDeleted, 0));
            if (userBalance == null || userBalance.getBalance().compareTo(amount) < 0) {
                throw new BusinessException("余额不足");
            }
            if (attempt == 2) throw new BusinessException("操作冲突，请重试");
        }
        
        // 记录余额变动
        BalanceLog log = new BalanceLog();
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setChangeAmount(amount.negate());
        log.setBalanceAfter(userBalance.getBalance());
        log.setChangeType("CONSUME");
        log.setRemark("订单支付");
        log.setOrderNo(orderNo);
        log.setCreateTime(LocalDateTime.now());
        balanceLogMapper.insert(log);
        
        RechargeServiceImpl.log.info("用户 {} 使用余额支付 {}，订单号 {}", userId, amount, orderNo);
    }
    
    /**
     * 分页查询用户余额变动流水。
     *
     * @param userId     用户 ID
     * @param tenantId   租户 ID
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @return 分页余额变动记录
     */
    @Override
    public Page<BalanceLog> listBalanceLogs(Long userId, Long tenantId, Integer pageNum, Integer pageSize) {
        Page<BalanceLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BalanceLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BalanceLog::getUserId, userId)
                .eq(BalanceLog::getTenantId, tenantId)
                .orderByDesc(BalanceLog::getCreateTime);
        return balanceLogMapper.selectPage(page, wrapper);
    }
    
    /**
     * 增加用户余额。
     * <p>
     * 若用户余额账户不存在则自动创建，否则累加余额和累计充值金额。
     * 同时记录一条余额变动流水。
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @param amount   入账金额
     * @param type     变动类型（如 RECHARGE）
     * @param reason   变动原因说明
     * @param orderNo  关联订单号
     */
    /**
     * 增加用户余额。
     * <p>
     * 若用户余额账户不存在则自动创建，否则累加余额和累计充值金额。
     * 同时记录一条余额变动流水。
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @param amount   入账金额
     * @param type     变动类型（如 RECHARGE）
     * @param reason   变动原因说明
     * @param orderNo  关联订单号
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
        log.setChangeAmount(amount);
        log.setBalanceAfter(userBalance.getBalance());
        log.setChangeType(type);
        log.setRemark(reason);
        log.setOrderNo(orderNo);
        log.setCreateTime(LocalDateTime.now());
        balanceLogMapper.insert(log);
    }
}
