package com.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payment.dto.WalletAccountVO;
import com.payment.dto.WalletLogVO;

import java.math.BigDecimal;

public interface UnifiedWalletService {
    WalletAccountVO getWallet(Long platformUserId);

    Page<WalletLogVO> listLogs(Long platformUserId, Integer current, Integer size);

    void credit(Long platformUserId, BigDecimal amount, String bizType, String bizNo, String remark);

    void debit(Long platformUserId, BigDecimal amount, String bizType, String bizNo, String remark);
}
