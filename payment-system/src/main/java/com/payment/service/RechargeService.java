package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.RechargeRuleDTO;
import com.payment.entity.BalanceLog;
import com.payment.entity.RechargeOrder;
import com.payment.entity.RechargeRule;

import java.math.BigDecimal;
import java.util.List;

/**
 * 充值服务接口。
 * <p>
 * 提供充值规则管理、充值订单创建、充值回调处理、用户余额查询
 * 及余额支付等核心充值业务能力。
 */
public interface RechargeService {

    /**
     * 获取商家充值规则。
     *
     * @param tenantId 租户 ID
     * @return 充值规则列表
     */
    List<RechargeRule> getRechargeRules(Long tenantId);

    /**
     * 设置商家充值规则。
     *
     * @param rules 充值规则列表
     */
    void setRechargeRules(List<RechargeRuleDTO> rules);

    /**
     * 创建充值订单。
     *
     * @param userId 用户 ID
     * @param ruleId 充值规则 ID
     * @return 充值订单实体
     */
    RechargeOrder createRechargeOrder(Long userId, Long ruleId);

    /**
     * 充值支付回调。
     * <p>
     * 支付成功后由支付回调链路调用，完成余额增加和订单状态更新。
     *
     * @param orderNo 充值订单号
     */
    void handleRechargeCallback(String orderNo);

    /**
     * 查询用户余额。
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return 用户当前余额（单位：元）
     */
    BigDecimal getUserBalance(Long userId, Long tenantId);

    /**
     * 使用余额支付。
     * <p>
     * 从用户余额中扣减指定金额，用于业务订单支付。
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @param orderNo  业务订单号
     * @param amount   扣减金额（单位：元）
     */
    void payWithBalance(Long userId, Long tenantId, String orderNo, BigDecimal amount);

    /**
     * 查询用户余额明细流水。
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 余额流水明细分页结果
     */
    Page<BalanceLog> listBalanceLogs(Long userId, Long tenantId, Integer pageNum, Integer pageSize);
}
