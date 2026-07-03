package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.Withdrawal;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提现申请 Mapper
 * <p>对应表：withdrawal，记录商户提现申请及审批状态</p>
 *
 * @author payment-system
 */
@Mapper
public interface WithdrawalMapper extends BaseMapper<Withdrawal> {
}
