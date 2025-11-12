package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.Withdrawal;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提现申请Mapper
 */
@Mapper
public interface WithdrawalMapper extends BaseMapper<Withdrawal> {
}
