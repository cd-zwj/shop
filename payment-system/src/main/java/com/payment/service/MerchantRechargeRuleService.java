package com.payment.service;

import com.payment.dto.V1MerchantRechargeRuleDTO;
import com.payment.entity.MerchantRechargeRule;

import java.util.List;

public interface MerchantRechargeRuleService {
    List<MerchantRechargeRule> listActiveRules(Long tenantId);

    /**
     * 查询所有租户的启用充值规则，用于统一钱包充值档位展示。
     */
    List<MerchantRechargeRule> listAllActiveRules();

    List<MerchantRechargeRule> listAllRules(Long tenantId);

    void replaceRules(Long tenantId, List<V1MerchantRechargeRuleDTO> rules);
}
