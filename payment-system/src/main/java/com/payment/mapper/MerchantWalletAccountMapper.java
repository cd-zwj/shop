package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MerchantWalletAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商户钱包账户 Mapper
 * <p>对应表：merchant_wallet_account，管理各商户级钱包的余额账户信息</p>
 *
 * @author payment-system
 */
@Mapper
public interface MerchantWalletAccountMapper extends BaseMapper<MerchantWalletAccount> {
}
