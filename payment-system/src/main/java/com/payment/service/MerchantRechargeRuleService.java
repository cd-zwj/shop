package com.payment.service;

import com.payment.dto.V1MerchantRechargeRuleDTO;
import com.payment.entity.MerchantRechargeRule;

import java.util.List;

/**
 * 商户充值规则服务接口。
 * <p>
 * 管理商户的充值档位配置，支持按租户查询、全量查询以及规则批量替换。
 * 充值规则定义了用户可选的充值金额档位及对应的赠送优惠。
 */
public interface MerchantRechargeRuleService {

    /**
     * 查询指定租户下所有启用的充值规则。
     *
     * @param tenantId 租户 ID
     * @return 启用状态的充值规则列表
     */
    List<MerchantRechargeRule> listActiveRules(Long tenantId);

    /**
     * 查询所有租户的启用充值规则，用于统一钱包充值档位展示。
     *
     * @return 所有租户的启用充值规则列表
     */
    List<MerchantRechargeRule> listAllActiveRules();

    /**
     * 查询指定租户下的所有充值规则（含启用和禁用）。
     *
     * @param tenantId 租户 ID
     * @return 充值规则列表
     */
    List<MerchantRechargeRule> listAllRules(Long tenantId);

    /**
     * 批量替换指定租户的充值规则。
     * <p>
     * 删除原有规则后重新插入，实现规则的全量更新。
     *
     * @param tenantId 租户 ID
     * @param rules    新的充值规则列表
     */
    void replaceRules(Long tenantId, List<V1MerchantRechargeRuleDTO> rules);
}
