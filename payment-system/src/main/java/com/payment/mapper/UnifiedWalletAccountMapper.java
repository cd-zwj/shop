package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.UnifiedWalletAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一钱包账户 Mapper
 * <p>对应表：unified_wallet_account，管理平台级统一钱包的余额账户信息</p>
 *
 * @author payment-system
 */
@Mapper
public interface UnifiedWalletAccountMapper extends BaseMapper<UnifiedWalletAccount> {
}
