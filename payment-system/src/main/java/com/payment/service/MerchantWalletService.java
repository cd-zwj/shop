package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.WalletAccountVO;
import com.payment.dto.WalletLogVO;

import java.math.BigDecimal;

public interface MerchantWalletService {
    WalletAccountVO getWallet(Long tenantId, Long platformUserId);

    Page<WalletLogVO> listLogs(Long tenantId, Long platformUserId, Integer current, Integer size);

    void credit(Long tenantId, Long platformUserId, BigDecimal amount, String bizType, String bizNo, String remark);

    void debit(Long tenantId, Long platformUserId, BigDecimal amount, String bizType, String bizNo, String remark);
}
