package com.payment.service;

import com.payment.dto.V1MerchantRechargeRuleDTO;
import com.payment.entity.MerchantRechargeRule;

import java.util.List;

public interface MerchantRechargeRuleService {
    List<MerchantRechargeRule> listActiveRules(Long tenantId);

    List<MerchantRechargeRule> listAllRules(Long tenantId);

    void replaceRules(Long tenantId, List<V1MerchantRechargeRuleDTO> rules);
}
