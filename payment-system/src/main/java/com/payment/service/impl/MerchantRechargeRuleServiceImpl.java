package com.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.payment.dto.V1MerchantRechargeRuleDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payment.entity.MerchantRechargeRule;
import com.payment.mapper.MerchantRechargeRuleMapper;
import com.payment.service.MerchantRechargeRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public List<MerchantRechargeRule> listAllRules(Long tenantId) {
        return merchantRechargeRuleMapper.selectList(new LambdaQueryWrapper<MerchantRechargeRule>()
                .eq(MerchantRechargeRule::getTenantId, tenantId)
                .orderByAsc(MerchantRechargeRule::getSortOrder)
                .orderByAsc(MerchantRechargeRule::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRules(Long tenantId, List<V1MerchantRechargeRuleDTO> rules) {
        merchantRechargeRuleMapper.delete(new QueryWrapper<MerchantRechargeRule>().lambda()
                .eq(MerchantRechargeRule::getTenantId, tenantId));

        int sortOrder = 1;
        for (V1MerchantRechargeRuleDTO ruleDTO : rules) {
            MerchantRechargeRule rule = new MerchantRechargeRule();
            rule.setTenantId(tenantId);
            rule.setRechargeAmount(ruleDTO.getRechargeAmount());
            rule.setGiftAmount(ruleDTO.getGiftAmount());
            rule.setGiftPoints(ruleDTO.getGiftPoints());
            rule.setStatus(Boolean.TRUE.equals(ruleDTO.getEnabled()) ? 1 : 0);
            rule.setSortOrder(ruleDTO.getSortOrder() == null ? sortOrder : ruleDTO.getSortOrder());
            merchantRechargeRuleMapper.insert(rule);
            sortOrder++;
        }
    }
}
