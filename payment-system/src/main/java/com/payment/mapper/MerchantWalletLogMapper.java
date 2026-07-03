package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MerchantWalletLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商户钱包流水日志 Mapper
 * <p>对应表：merchant_wallet_log，记录商户钱包账户的每一笔资金变动明细</p>
 *
 * @author payment-system
 */
@Mapper
public interface MerchantWalletLogMapper extends BaseMapper<MerchantWalletLog> {
}
