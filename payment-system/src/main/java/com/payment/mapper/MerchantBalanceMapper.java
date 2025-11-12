package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.MerchantBalance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家余额Mapper
 */
@Mapper
public interface MerchantBalanceMapper extends BaseMapper<MerchantBalance> {
}
