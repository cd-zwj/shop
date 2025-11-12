package com.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payment.entity.BalanceLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 余额明细Mapper
 */
@Mapper
public interface BalanceLogMapper extends BaseMapper<BalanceLog> {
}
