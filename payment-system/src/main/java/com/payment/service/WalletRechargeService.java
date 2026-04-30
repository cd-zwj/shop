package com.payment.service;

import com.payment.dto.CreateMerchantWalletRechargeDTO;
import com.payment.dto.CreateUnifiedWalletRechargeDTO;
import com.payment.dto.RechargePaymentVO;

public interface WalletRechargeService {
    RechargePaymentVO createUnifiedRecharge(Long platformUserId, CreateUnifiedWalletRechargeDTO dto);

    RechargePaymentVO createMerchantRecharge(Long tenantId, Long platformUserId, CreateMerchantWalletRechargeDTO dto);

    void handleRechargeSuccess(String rechargeNo);
}
