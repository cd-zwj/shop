package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UnifiedWalletLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一钱包流水日志 Mapper
 * <p>对应表：unified_wallet_log，记录统一钱包账户的每一笔资金变动明细（充值、扣款、退款等）</p>
 *
 * @author payment-system
 */
@Mapper
public interface UnifiedWalletLogMapper extends BaseMapper<UnifiedWalletLog> {
}
