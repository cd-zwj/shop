package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.RechargeRuleDTO;
import com.payment.entity.BalanceLog;
import com.payment.entity.RechargeOrder;
import com.payment.entity.RechargeRule;

import java.math.BigDecimal;
import java.util.List;

/**
 * 充值服务接口
 */
public interface RechargeService {
    
    /**
     * 获取商家充值规则
     * @param tenantId 租户ID
     * @return 充值规则列表
     */
    List<RechargeRule> getRechargeRules(Long tenantId);
    
    /**
     * 设置商家充值规则
     * @param rules 充值规则列表
     */
    void setRechargeRules(List<RechargeRuleDTO> rules);
    
    /**
     * 创建充值订单
     * @param userId 用户ID
     * @param ruleId 充值规则ID
     * @return 充值订单
     */
    RechargeOrder createRechargeOrder(Long userId, Long ruleId);
    
    /**
     * 充值支付回调
     * @param orderNo 订单号
     */
    void handleRechargeCallback(String orderNo);
    
    /**
     * 查询用户余额
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 余额
     */
    BigDecimal getUserBalance(Long userId, Long tenantId);
    
    /**
     * 使用余额支付
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @param orderNo 订单号
     * @param amount 金额
     */
    void payWithBalance(Long userId, Long tenantId, String orderNo, BigDecimal amount);
    
    /**
     * 余额明细
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 余额明细分页
     */
    Page<BalanceLog> listBalanceLogs(Long userId, Long tenantId, Integer pageNum, Integer pageSize);
}
