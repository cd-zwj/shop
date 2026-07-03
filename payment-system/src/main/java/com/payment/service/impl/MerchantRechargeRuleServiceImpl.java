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

    /**
     * 查询指定商户下启用的充值规则列表（按排序权重升序）。
     *
     * @param tenantId 租户 ID
     * @return 启用状态的充值规则列表
     */
    @Override
    public List<MerchantRechargeRule> listActiveRules(Long tenantId) {
        return merchantRechargeRuleMapper.selectList(new LambdaQueryWrapper<MerchantRechargeRule>()
                .eq(MerchantRechargeRule::getTenantId, tenantId)
                .eq(MerchantRechargeRule::getStatus, 1)
                .orderByAsc(MerchantRechargeRule::getSortOrder));
    }

    /**
     * 查询所有商户下启用的充值规则（跨租户）。
     *
     * @return 所有启用状态的充值规则列表
     */
    @Override
    public List<MerchantRechargeRule> listAllActiveRules() {
        return merchantRechargeRuleMapper.selectList(new LambdaQueryWrapper<MerchantRechargeRule>()
                .eq(MerchantRechargeRule::getStatus, 1)
                .orderByAsc(MerchantRechargeRule::getSortOrder));
    }

    /**
     * 查询指定商户下所有充值规则（包含启用和禁用）。
     *
     * @param tenantId 租户 ID
     * @return 全部充值规则列表
     */
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
