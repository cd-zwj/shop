package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.MerchantRechargeRule;
import com.payment.mapper.MerchantRechargeRuleMapper;
import com.payment.service.MerchantRechargeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商户钱包充值规则服务。
 */
@Service
@RequiredArgsConstructor
public class MerchantRechargeRuleServiceImpl implements MerchantRechargeRuleService {

    private final MerchantRechargeRuleMapper merchantRechargeRuleMapper;

    @Override
    public List<MerchantRechargeRule> listActiveRules(Long tenantId) {
        return merchantRechargeRuleMapper.selectList(new LambdaQueryWrapper<MerchantRechargeRule>()
                .eq(MerchantRechargeRule::getTenantId, tenantId)
                .eq(MerchantRechargeRule::getStatus, 1)
                .orderByAsc(MerchantRechargeRule::getSortOrder));
    }
}
