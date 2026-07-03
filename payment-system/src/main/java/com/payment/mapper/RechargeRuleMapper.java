package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.RechargeRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 充值规则 Mapper
 * <p>对应表：recharge_rule，管理平台级钱包充值规则（如充值梯度、赠送比例等）</p>
 *
 * @author payment-system
 */
@Mapper
public interface RechargeRuleMapper extends BaseMapper<RechargeRule> {
}
