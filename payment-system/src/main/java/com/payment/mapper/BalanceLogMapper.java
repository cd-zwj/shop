package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.BalanceLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 余额变动日志 Mapper
 * <p>对应表：balance_log，记录商户余额的每一笔进出明细（收入、提现、退款等）</p>
 *
 * @author payment-system
 */
@Mapper
public interface BalanceLogMapper extends BaseMapper<BalanceLog> {
}
