package com.payment.service;

import com.payment.entity.MerchantRechargeRule;

import java.util.List;

public interface MerchantRechargeRuleService {
    List<MerchantRechargeRule> listActiveRules(Long tenantId);
}
