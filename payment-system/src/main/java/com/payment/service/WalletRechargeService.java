package com.payment.service;

import com.payment.dto.CreateMerchantWalletRechargeDTO;
import com.payment.dto.CreateUnifiedWalletRechargeDTO;
import com.payment.dto.RechargePaymentVO;

/**
 * 钱包充值服务接口。
 * <p>
 * 负责统一钱包和商户钱包的充值流程管理，包括充值订单创建、
 * 支付参数生成以及充值成功后的回调处理。
 * 充值金额到账由充值成功回调触发，确保资金安全。
 */
public interface WalletRechargeService {

    /**
     * 创建统一钱包充值订单。
     * <p>
     * 根据用户选择的充值档位生成充值订单，并调用支付渠道获取支付参数。
     *
     * @param platformUserId 平台用户 ID
     * @param dto            统一钱包充值请求 DTO，包含充值金额/档位等信息
     * @return 充值支付视图对象，包含充值单号和第三方支付参数
     */
    RechargePaymentVO createUnifiedRecharge(Long platformUserId, CreateUnifiedWalletRechargeDTO dto);

    /**
     * 创建商户钱包充值订单。
     *
     * @param tenantId       租户 ID
     * @param platformUserId 平台用户 ID
     * @param dto            商户钱包充值请求 DTO，包含充值金额/档位等信息
     * @return 充值支付视图对象，包含充值单号和第三方支付参数
     */
    RechargePaymentVO createMerchantRecharge(Long tenantId, Long platformUserId, CreateMerchantWalletRechargeDTO dto);

    /**
     * 处理充值成功回调。
     * <p>
     * 支付渠道回调成功后调用，完成充值金额到账（钱包余额增加）并记录流水。
     *
     * @param rechargeNo 充值订单号
     */
    void handleRechargeSuccess(String rechargeNo);
}
