package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MerchantRechargeRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商户充值规则 Mapper
 * <p>对应表：merchant_recharge_rule，管理商户自定义的充值规则配置</p>
 *
 * @author payment-system
 */
@Mapper
public interface MerchantRechargeRuleMapper extends BaseMapper<MerchantRechargeRule> {
}
