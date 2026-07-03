package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MerchantBalance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商户余额 Mapper
 * <p>对应表：merchant_balance，存储商户可提现/可使用余额的核心账户信息</p>
 *
 * @author payment-system
 */
@Mapper
public interface MerchantBalanceMapper extends BaseMapper<MerchantBalance> {
}
